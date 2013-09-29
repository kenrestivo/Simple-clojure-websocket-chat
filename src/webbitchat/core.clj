(ns webbitchat.core
  (:use [clojure.tools.logging :only (info error)])
  (:require [clojure.string :as string]
            [environ.core :as env]
            [amalloy.ring-buffer :as rb]
            [webbitchat.websocketwrap :as wsr])
  (:import [org.webbitserver WebServer WebServers WebSocketHandler WebSocketConnection]
           )
  (:gen-class :main true))


;; TODO: extend-protocol c to the assoc protocol


(defonce conn-table (atom {}))

(defonce csrv (atom nil))

(defonce backlog (atom (rb/ring-buffer 10)))

(defn log [m]
  (swap! backlog (partial cons m)))

(defn send-all [m]
  (doseq [c (keys @conn-table)]
    (wsr/sendm c m))
  (log m))



(defn  on-open [ c]
  (let [ip  (wsr/ipaddr c)]
    (swap! conn-table #(assoc % c {:ip ip}))
    (info (str ip " connecting " (.data c)))))



(defn on-close [^WebSocketConnection cobj]
  (let [{:keys [username ip] :as m} (get @conn-table cobj) ;; obj already exists and has data
        m {:action :LEAVE
           :username username}] 
    (info (format "%s %s leaving"  ip username))
    (send-all m )
    (swap! conn-table #(dissoc % cobj))))


;;; todo: could wrap this in a transaction maybe
(defn usernames
  "get all the usernames as a set"
  []
  (set (map :username (vals @conn-table))))


(defn gen-unique [name coll & num]
  "generates a name unique to the collection supplied"
  (let [num-unsequed  (if (seq? num) (first num) num)
        num-seeded (or num 0)
        name-combined (str name num-unsequed)]
    (if (contains? coll name-combined)
      (recur coll name (inc num-seeded))
      name-combined)))


(defn constrain-username
  "because some people think it's cute to come up with goofy names"
  [name]
  (let [n (string/trim name)
        l (.length n)]
    (.substring n 0 (min l 50))))



(defn forward-all
  [msg _ {:keys [username] :as cmap}]
  (send-all (assoc msg :username username)))

(defmulti send-multi
  (fn [msg cobj cmap]
    (-> msg :action keyword)))


;;; can't user heirarchies here, because the conversion to keywords is not ns-qualified
(defmethod send-multi :SAY
  [msg _ cmap]
  (forward-all msg nil cmap))

;;; can't user heirarchies here, because the conversion to keywords is not ns-qualified
(defmethod send-multi :SPRAY
  [msg _ cmap]
  (forward-all msg nil cmap))

(defmethod send-multi :USERLIST
  [_ cobj _]
  (wsr/sendm cobj {:action :USERLIST
                   :userlist (usernames)}))


(defmethod send-multi :LOGIN
  [m cobj cmap]
  (let [username (-> m
                     :loginUsername
                     constrain-username 
                     (gen-unique (usernames)))]
    (swap! conn-table #(assoc-in % [cobj :username] username))
    (send-multi {:action :USERLIST} cobj cmap)
    (doseq [m (reverse @backlog)]
      (wsr/sendm cobj m))
    (send-all {:action :JOIN
               :username username})))


(defmethod send-multi :default
  [m cobj cmap]
  (wsr/sendm cobj {:action :ERROR
                   :message (str "There is no such action as " (:action m))}))


(defn on-message [^WebSocketConnection cobj m]
  ;;(reset! res j) ;; debug only
  (let [{:keys [ip username] :as cmap} (get @conn-table cobj)]
    (info (format "%s %s %s" ip username m))
    (send-multi m cobj cmap)))




(defn start-server [srv]
  (.add srv "/chatsocket"
        (proxy [WebSocketHandler] []
          ;; creating the wrapped object only in open. beyond that we have it already.
          (onOpen [^WebSocketConnection c] (on-open c))
          (onClose [^WebSocketConnection c] (on-close c))
          ;; TODO: catch json exception and send a response intelligently. maybe even remove user!
          (onMessage [^WebSocketConnection c j] (on-message c (wsr/decode j)))))
  (.start srv))




;;;;;;;;;;;;;


(defn -main [& m]
  (let [port (get env/env :port 9876)]
    (info "starting server on " port)
    (reset! csrv (WebServers/createWebServer port))
    (start-server @csrv)
    (println "server started on port " port)))