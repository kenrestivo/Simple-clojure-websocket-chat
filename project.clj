(defproject webbitchat "1.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.webbitserver/webbit "0.4.15"]
                 [cheshire "5.1.1"]
                 [environ "0.3.0"]
                 [amalloy/ring-buffer "1.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [log4j "1.2.16"]]
  :aot [webbitchat.core]
  :profiles {:server { :injections [(do (require 'webbitchat.core)
                                        (webbitchat.core/-main))]}}
  :main webbitchat.core
  :aliases {"prod" ["with-profile" "user,server"
                    "do"
                    "trampoline" "repl" ":headless"]
            "tr" ["with-profile" "user,dev,server"
                  "trampoline" "repl" ":headless"]}
)