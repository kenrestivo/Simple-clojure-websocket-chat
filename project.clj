(defproject webbitchat "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.webbitserver/webbit "0.4.6"]
                 [cheshire "3.0.0"]
                 [org.clojure/tools.logging "0.2.3"]]
  :repl-port 7898
  :main webbitchat.core)