(ns webbitchat.websocketwrap
  (:import [org.webbitserver WebServer WebServers WebSocketHandler WebSocketConnection])
  (:require [cheshire.core :as json]
            [clojure.walk :as walk]
            ))

(defn decode [j]
  (json/decode j true))



(defn sendm [^WebSocketConnection c m]
  (->> m
       json/encode
       (.send c)))


(defn ipaddr [^WebSocketConnection c]
  "Because dealing with java is like dealing with the Italian bureaucracy"
  (-> c .httpRequest  .remoteAddress .getAddress .getHostAddress))

