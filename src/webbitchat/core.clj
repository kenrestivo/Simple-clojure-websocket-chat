(ns webbitchat.core
  (:require [clj-json.core :as json]
            [clojure.string :as s])
  (:import [org.webbitserver WebServer WebServers WebSocketHandler]
           [org.webbitserver.handler StaticFileHandler]))


(def res (atom nil))


(def conn (atom #{}))

(defn send-all [m]
  (let [j (json/generate-string m)]
    (doseq [c @conn]
      (.send c j))))

(defn  on-open [c]
  (println c)
  (swap! conn #(conj % c)))
  

(defn on-close [c]
  (println c)
  (send-all {:action "LEAVE"
             :username (.data c "username")})
  (swap! conn #(disj % c)))





(defn usernames []
  "show all the usernames"
  (map #(.data % "username") @conn))




;; TODO: catch json exception and send a response intelligently
(defn on-message [c j]
  (reset! res j) ;; debug only
  (println (str "i gots " j))
  (let [m (json/parse-string j)
        action (m "action")]
    (if (contains? #{"SAY" "SPRAY"} action)
      (send-all (assoc m :username (.data c "username"))))
    (if (= action "LOGIN" )
      (do
        (.data c "username" (m "loginUsername"))
        (send-all {:action "JOIN"
                   :username (.data c "username")})))))




(def csrv (WebServers/createWebServer 9876))

(.add csrv "/chatsocket"
      (proxy [WebSocketHandler] []
        (onOpen [c] (on-open c))
        (onClose [c] (on-close c))
        (onMessage [c j] (on-message c j))))




;;;;;;;;;;;;;


(defn -main [& m]
  (.start csrv))