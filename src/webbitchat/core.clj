(ns webbitchat.core
  (:use [cheshire.core]
        [clojure.tools.logging :only (info error)])
  (:require [clojure.string :as string])
  (:import [org.webbitserver WebServer WebServers WebSocketHandler])
  (:gen-class :main true))



(def conn (atom #{}))

(defn send-all [m]
  (let [j (encode m)]
    (doseq [c @conn]
      (.send c j))))



(defn ipaddr [c]
  "Because dealing with java is like dealing with the Italian bureaucracy"
  (-> (.httpRequest c)  .remoteAddress .getAddress .getHostAddress))

(defn  on-open [c]
  (swap! conn #(conj % c))
  (.data c "ip" (ipaddr c))
  (info (str (.data  c "ip") " joining")))

  

(defn on-close [c]
  (info (format "%s %s leaving"  (.data c "ip") (.data c "username")))
  (send-all {:action "LEAVE"
             :username (.data c "username")})
  (swap! conn #(disj % c)))


;;; todo: could wrap this in a transaction maybe
(defn usernames
  "get all the usernames as a set"
  []
  (set (map #(.data % "username") @conn)))


(defn gen-unique [coll name & num]
  "generates a name unique to the collection supplied"
  (let [num-unsequed  (if (seq? num) (first num) num)
        num-seeded (or num 0)
        name-combined (str name num-unsequed)]
    (if (contains? coll name-combined)
      (recur coll name (inc num-seeded))
      name-combined)))


(defn say-or-spray [ m c]
  (send-all (assoc m :username (.data c "username"))))




(defn userlist [m c]
  (.send c (encode
            {:action "USERLIST"
             :userlist (usernames)})))


(defn constrain-username
  "because some people think it's cute to come up with goofy names"
  [name]
  (string/trim name))


(defn login [m c]
  (.data c "username"
         (gen-unique (usernames) (constrain-username (m "loginUsername"))))
  (userlist m c)
  (send-all {:action "JOIN"
             :username (.data c "username")}))


(defn dispatch [m c action]
  (cond
   (contains? #{"SAY" "SPRAY"} action) say-or-spray
   (= action "LOGIN") login
   (= action "USERLIST") userlist))

     


;; TODO: catch json exception and send a response intelligently
(defn on-message [c j]
  ;;(reset! res j) ;; debug only
  (info (format "%s %s %s" (.data c "ip") (.data c "username") j))
  (let [m (decode j)
        action (m "action")
        f (dispatch m c action)]
    (f m c)))




(def csrv (WebServers/createWebServer 9876))

(.add csrv "/chatsocket"
      (proxy [WebSocketHandler] []
        (onOpen [c] (on-open c))
        (onClose [c] (on-close c))
        (onMessage [c j] (on-message c j))))




;;;;;;;;;;;;;


(defn -main [& m]
  (.start csrv))