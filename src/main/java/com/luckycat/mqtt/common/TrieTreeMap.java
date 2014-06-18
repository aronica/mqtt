package com.luckycat.mqtt.common;

import io.netty.util.internal.ConcurrentSet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: fafu
 * Date: 14-6-3
 * Time: 下午3:08
 * This class is
 */
public class TrieTreeMap<V> {
    private Node root;
    public ConcurrentHashMap<String,Node<V>> nodes = new ConcurrentHashMap<String,Node<V>>();
    public TrieTreeMap(){
        root = new Node("/",null);
        nodes.put("/",root);
    }

//    public void put(String key,V v){
//        if(key == null){
//            throw new IllegalArgumentException("Key should not be null");
//        }
//        if("".equals(key)){
//            throw new IllegalArgumentException("Key should not be empty string.");
//        }
//        key = key.trim();
//        if(key.charAt(0)!='/'){
//            key = "/" + key;
//        }
//        if(key.endsWith("/")){
//            key = key.substring(0,key.length()-1);
//        }
//        Node<V> node = root;
//        String parentPath = key.substring(0,key.lastIndexOf("/"));
//        Node<V> tmpNode = nodes.get(parentPath);
//        while(tmpNode == null&&parentPath.indexOf("/")>=0){
//            parentPath = parentPath.substring(0,parentPath.lastIndexOf("/"));
//            tmpNode = nodes.get(parentPath);
//        }
//        if(tmpNode == null){
//
//        }
//        String sub = key.substring(parentPath.length());
//        String[] split = sub.split("/");
//        node = tmpNode;
//        synchronized(node.lock){
//            for(int i = 0;i<split.length;i++){
//                if(!node.child.containsKey(split[i])){
//                    String path = node.path+"/"+split[i];
//                    node = new Node(split[i],null,node);
//                    nodes.put(path,node);
//                } else{
//                    node = node.child.get(split[i]);
//                }
//            }
//        }
//    }

    public V get(String key){
        Set<V> v =  gets(key);
        if(v == null||v.size() == 0)return null;
        if(v.size() > 0){
            throw new RuntimeException("Result return multiple results");
        }
        return (V)v.toArray()[0];
    }

    public Set<V> gets(String key){
        Set<V> nodes = new HashSet<V>();
        get(root,key.split("/"),nodes);
        return nodes;
    }

    public void subscribe(String key,V v){
        if(key == null||key.trim().length() == 0){
            throw new IllegalArgumentException("Subscription key should not be nil or empty string.");
        }
        int idx = key.indexOf("#");
        if(idx>-1&&idx != key.length()-1){
            throw new IllegalArgumentException("Multiple placeholder '#' should be the last character.");
        }
        if(idx>0&&key.charAt(idx-1)!='/'){
            throw new IllegalArgumentException("Multiple placeholder '#' should be in a single level like '#' or '/home/#'.");
        }
        key = key.trim();
        String sub = key;
        String[] split = sub.split("/");
        Node<V> node = root;
        for(int i = 0;i<split.length;i++){
            if(split[i].length() == 0){
                throw new IllegalArgumentException("Subscription key should not be nil or empty string.");
            }
            if(!node.child.containsKey(split[i])){
                if(split[i].trim().indexOf("+")>-1&&split[i].length()>1){
                    throw new IllegalArgumentException("Single placeholder '+' should be in a single level like '#' or '/home/+/bin'.");
                }
                split[i] = split[i].trim();
                String path = node.path+"/"+split[i];
                Node<V> child = new Node(split[i],node);
                node = node.child.putIfAbsent(split[i],child);
                if(node == null){
                    node = child;
                }
                nodes.putIfAbsent(path,node);
            } else{
                node = node.child.get(split[i]);
            }
        }
        node.equalSet.add(v);
    }


    public Set<V> getKeys(String key){
        if(key == null||key.trim().length() == 0){
            throw new IllegalArgumentException("Subscription key should not be nil or empty string.");
        }
        key = key.trim();
        Set<V> nodes = new HashSet<V>();
        get(root,key.split("/"),nodes);
        return nodes;
    }

    private void get(Node<V> currentRoot ,String[] split,Set<V> vs){
        Node<V> node = currentRoot;
        for(int i = 0;i<split.length;i++){
            if(split[i].length() == 0){
                throw new IllegalArgumentException("Subscription key should not be nil or empty string.");
            }
            vs.addAll(node.child.get("#").equalSet);
//            Node<V> single = node.child.get("+");
            Node<V> single = node.child.get("+");
            if(single!=null){
                if(i == split.length - 1) {
                    vs.addAll(single.equalSet);
                } else if(single.child.size()>0){
                    //for(Node<V> child:single.child.values()){
                        get(single,Arrays.copyOfRange(split,i+1,split.length),vs);
                    //}
                }

            }
            Node<V> childNode = node.child.get(split[i]);
            if(childNode == null)return;
            node = childNode;
        }
        vs.addAll(node.equalSet);
    }

    private List<Node<V>> getChildrenNodes(Node<V> node){
        List<Node<V>> nodes = new ArrayList<Node<V>>();
        nodes.add(node);
        if(node.child.size() == 0)return nodes;
        nodes.addAll(node.child.values());
        for(Node<V> n:node.child.values()){
            nodes.addAll(getChildrenNodes(n));
        }
        return nodes;
    }

    public static void main(String[] args) throws IOException {
        TrieTreeMap<String> map = new TrieTreeMap<String>();
        map.subscribe("news/+/ibm/server/x86", "news/+/ibm/server/x86");
        map.subscribe("news/tech/+/server/power", "news/tech/ibm/+/power");
        map.subscribe("news/+/+/server/x86", "news/+/+/server/x86");
        map.subscribe("news/tech/#", "news/tech");
        batchPrint(map.gets("news/tech/#"),System.out);
    }

    protected static void batchPrint(Set<?> collections,OutputStream os) throws IOException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os));
        for(Iterator it = collections.iterator();it.hasNext();){
            Object o = it.next();
            w.write(o.toString());
            w.write('\n');
            w.flush();
        }
    }

    public void unsubscribe(String key, V v) {
        if(key == null||key.trim().length() == 0){
            throw new IllegalArgumentException("Unsubscribe value should not be nil.");
        }
        int idx = key.indexOf("#");
        if(idx>-1&&idx != key.length()-1){
            throw new IllegalArgumentException("Multiple placeholder '#' should be the last character.");
        }
        if(idx>0&&key.charAt(idx-1)!='/'){
            throw new IllegalArgumentException("Multiple placeholder '#' should be in a single level like '#' or '/home/#'.");
        }
        if(v == null){
            throw new IllegalArgumentException("Multiple placeholder '#' should be in a single level like '#' or '/home/#'.");
        }
        key = key.trim();
        String sub = key;
        String[] split = sub.split("/");
        Node<V> node = root;
        for(int i = 0;i<split.length;i++){
            if(split[i].length() == 0){
                throw new IllegalArgumentException("Subscription key should not be nil or empty string.");
            }
            if(!node.child.containsKey(split[i])){
                if(split[i].trim().indexOf("+")>-1&&split[i].length()>1){
                    throw new IllegalArgumentException("Single placeholder '+' should be in a single level like '#' or '/home/+/bin'.");
                }
                split[i] = split[i].trim();
                Node<V> child = node.child.get(split[i]);
                if(child != null){
                    node = child;
                }else{
                    break;
                }
            } else{
                node = node.child.get(split[i]);
            }
        }
        node.equalSet.remove(v);
    }

    private static class Node<V>{
        public ConcurrentHashMap<String,Node<V>> child;
        public String key;
        public Node parent;
        public String path;
        public Object lock;

        public Node(String key,Node<V> parent){
            child = new ConcurrentHashMap<String,Node<V>>();
            child.put("#",null);
            this.key = key;
            this.parent = parent;
            this.lock = new Object();
            if(this.parent != null){
                this.path = (parent.path.equals("/")?"":parent.path)+"/"+key;
            } else{
                this.path = key;
            }
        }

        public ConcurrentSet<V> equalSet = new ConcurrentSet<V>();
        public ConcurrentSet<V> multipleSet = new ConcurrentSet<V>();
//        public ConcurrentSet<Subscription> singleSet = new ConcurrentSet<Subscription>();
    }
}
