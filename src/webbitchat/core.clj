(ns webbitchat.core
  (:use [cheshire.core])
  (:require [clojure.string :as s])
  (:import [org.webbitserver WebServer WebServers WebSocketHandler]))


(def log (atom nil))

(def conn (atom #{}))

(defn send-all [m]
  (let [j (encode m)]
    (doseq [c @conn]
      (.send c j))))


(defn logprint [x]
  (binding [*out* @log]
    (println x)))


(defn ipaddr [c]
  " because java is so miserable"
  (let [htr (.httpRequest c)]
    (-> htr .remoteAddress .getAddress .getHostAddress)))

(defn  on-open [c]
  (swap! conn #(conj % c))
  (.data c "ip" (ipaddr c))
  (logprint (str (.data  c "ip") " joining")))

  

(defn on-close [c]
  (logprint (format "%s %s leaving"  (.data c "ip") (.data c "username")))
  (send-all {:action "LEAVE"
             :username (.data c "username")})
  (swap! conn #(disj % c)))



(defn usernames []
  "show all the usernames"
  (map #(.data % "username") @conn))



(defn say-or-spray [ m c]
  (send-all (assoc m :username (.data c "username"))))

(defn login [m c]
  (.data c "username" (m "loginUsername"))
  (send-all {:action "JOIN"
             :username (.data c "username")}))


(defn userlist [m c]
  (.send c (encode
            {:userlist (usernames)})))


(defn dispatch [m c action]
  (cond
   (contains? #{"SAY" "SPRAY"} action) say-or-spray
   (= action "LOGIN") login
   (= action "USERLIST") userlist))

     


;; TODO: catch json exception and send a response intelligently
(defn on-message [c j]
  ;;(reset! res j) ;; debug only
  (logprint (format "%s %s %s" (.data c "ip") (.data c "username") j))
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



(defn logsetup [x]
  (reset! log (clojure.java.io/writer x)))

;;;;;;;;;;;;;


(defn -main [& m]
  (logsetup "webbit.log")
  (.start csrv))