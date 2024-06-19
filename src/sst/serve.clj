(ns sst.serve
  (:require
    [medley.core :as md]
    [mount.core :as mount :refer [defstate]]
    [sst.ui.app]
    [reitit.ring :as r.ring]
    [ring.util.response :as resp]
    [stuffs.util :as su]
    [stuffs.prepl]
    [org.httpkit.server :as http-kit]
    [hyperfiddle.electric :as e]
    [hyperfiddle.electric-httpkit-adapter :as electric]
    ;; require order matters
    [ring.middleware.resource :refer [wrap-resource]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.not-modified :refer [wrap-not-modified]]))

(defn document
  [{:as opts :keys [js styles links meta body title]}]
  [:html
   [:head
    (for [m meta]
      [:meta m])
    (for [s styles]
      [:style {:type                    "text/css"
               :dangerouslySetInnerHTML {:__html s}}])
    (for [l links]
      [:link (if (map? l)
               l
               {:rel "stylesheet" :href l})])
    (when title [:title title])]
   [:body
    body
    (for [{:keys [src script]} js]
      [:script
       (if src
         {:src src}
         {:dangerouslySetInnerHTML {:__html script}})])]])

(defn html [{:as opts :keys [js styles links meta body]}]
  [:<>
   [:meta {:charset "UTF-8"}]
   [document opts]])

(defn sst-index [_]
  {:status  200
   :headers {"content-type" "text/html"}
   :body    (su/hiccup->html
              [html
               {:title "SpreadSheesh Talk"
                :meta  [{:name    "viewport"
                         :content "width=device-width, initial-scale=1"}]
                :links [#_"/css/tachyons.css"
                        #_{:rel  "manifest"
                           :href "/manifest.json"}
                        "/css/styles.css"
                        ]
                :js    [{:src "/js/compiled/talk/main.js"}
                        #_{:src "https://cdn.tailwindcss.com"}
                        #_{:src "/js/tailwind.js"}]
                :body  [:div#root]}])})

(defn resource-handler [handler]
  (-> handler
      (wrap-resource "public")
      (wrap-content-type)
      (wrap-not-modified)))

(defn wrap-electric-websocket [next-handler]
  (fn [req]
    ((electric/wrap-electric-websocket
       next-handler
       (fn boot [_req]
         (e/boot-server {} sst.ui.app/Main)))
     req)))

(def router
  (r.ring/router
    [["/"
      {:get (fn [_] (resp/redirect "/talk"))}]
     ["/talk*"
      {:middleware [wrap-electric-websocket]
       :get        sst-index}]
     ]))


(def handler
  (r.ring/ring-handler
    router
    (r.ring/create-default-handler)
    {:middleware [#(wrap-defaults %
                                  (-> site-defaults
                                      (assoc-in [:security :anti-forgery] false)
                                      (md/dissoc-in [:security :frame-options])
                                      (assoc-in [:session :cookie-attrs :same-site] :lax)))
                  #_(fn [h]
                    #(h (su/dtap %)))
                  resource-handler]}))

(defonce init-opts
  (atom {:port 3200}))

(def cli-opts
  [["-p" "--port PORT" "Port number"
    :default (:port @init-opts)
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]])

(defstate server
  :start (http-kit/run-server
           handler
           {:port                 (:port @init-opts)
            :legacy-return-value? false})
  :stop (http-kit/server-stop! server))

(comment
  (mount/start)
  (mount/stop)
  )

(defn -main
  [& args]
  (println "SpreadSheesh booting up...")
  (mount/start)
  (println "SpreadSheesh ready... http://localhost:3200"))


(comment
  ;; dev
  (do
    (-> (mount/with-args {})
        (mount/start))
    #_(mount/start)
    (future
      (do
        (def shadow-start! @(requiring-resolve 'shadow.cljs.devtools.server/start!))
        (def shadow-stop! @(requiring-resolve 'shadow.cljs.devtools.server/stop!))
        (def shadow-watch @(requiring-resolve 'shadow.cljs.devtools.api/watch))
        (do
          (shadow-start!)
          (shadow-watch :sst))))
    (comment
      (shadow-stop!)
      (def shadow-release @(requiring-resolve 'shadow.cljs.devtools.api/release))
      (shadow-release :sst))))
