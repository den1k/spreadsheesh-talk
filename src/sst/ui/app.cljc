(ns sst.ui.app
  (:require [clojure.string :as str]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [sst.ui.talk :as talk]
    ;[tesserae.ui.globals :as g]
    ;[tesserae.ui.views :as views]
    #_[tesserae.ui.temp-views :as temp-views]
            [missionary.core :as m]
            [stuffs.js-interop :as j]
            [reitit.core :as rr]
            [reitit.coercion :as rc]
            [reitit.coercion.spec :as rss]
            #?(:cljs [reitit.frontend.easy :as rfe])))


(def router
  (rr/router
    [["/talk"
      ["" :talk]]]
    {:compile rc/compile-request-coercers
     :data    {:coercion rss/coercion}}))

#?(:cljs
   (defn set-page-title! [route-match]
     (j/assoc! js/document
               :title
               (->> route-match :data :name (str "SpreadSheesh – ")))))

(e/def re-router
  (e/client
    (->> (m/observe
           (fn [!]
             (rfe/start!
               router
               !
               {:use-fragment false})))
         (m/relieve {})
         new)))

(e/defn App []
  (e/client
    (dom/h1 (dom/text "HELLOOO"))))

(e/defn Main []
  (e/client
    (binding [dom/node (dom/by-id "root")]
      (let [{:as match :keys [data query-params path-params]} re-router]
        #_(set-page-title! match)
        #_(new views/App)
         ;(new App)
        (new talk/Slides)
        ))))

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
  (when reactor (reactor))                                  ; teardown
  (set! reactor nil))
