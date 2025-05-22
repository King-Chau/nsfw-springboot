package com.nsfw.image;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.proto.GraphDef;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * load TensorFlow GraphDef (.pb)
 */
public class ModelHolder {
    private static volatile Graph graph;
    private static volatile Session session;
    private static final String PB_PATH = "models/nsfw_mobilenet.pb";

    private ModelHolder() {  }

    /**
     * get singleton Session
     */
    public static Session getSession() {
        if (session == null) {
            synchronized (ModelHolder.class) {
                if (session == null) {
                    try {
                        byte[] graphBytes = Files.readAllBytes(Paths.get(PB_PATH));
                        GraphDef graphDef = GraphDef.parseFrom(graphBytes);

                        graph = new Graph();
                        graph.importGraphDef(graphDef);
                        session = new Session(graph);
                        System.out.println(".pb 模型已加载: " + PB_PATH);
                    } catch (Exception e) {
                        throw new RuntimeException("加载 .pb 模型失败: " + PB_PATH, e);
                    }
                }
            }
        }
        return session;
    }

    /**
     * close Session
     */
    public static void close() {
        if (session != null) {
            session.close();
            graph.close();
            session = null;
            graph = null;
            System.out.println(".pb 模型已关闭");
        }
    }
}
