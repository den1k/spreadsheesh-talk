(ns sst.ui.app
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [sst.ui.talk :as talk]))

(e/defn Main []
  (e/client
    (binding [dom/node (dom/by-id "root")]
      (new talk/Slides))))

#?(:cljs
   (def boot-client
     (e/boot-client {} Main)))

(defonce reactor nil)

#?(:cljs
   (defn ^:dev/after-load ^:export start []
     (assert (nil? reactor) "reactor already running")
     (set! reactor (boot-client
                     #(js/console.log "Reactor success:" %)
                     #(js/console.error "Reactor failure:" %)))))

(defn ^:dev/before-load stop []
  (when reactor (reactor))
  (set! reactor nil))
