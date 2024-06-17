(ns sst.server
  (:require
    [mount.core :as mount :refer [defstate]]
    [sst.ui.app]
    [reitit.ring :as r.ring]
    [ring.util.response :as resp]
    [stuffs.util :as su]
    [stuffs.env :as env]
    [stuffs.prepl]
    [org.httpkit.server :as http-kit]
    [hyperfiddle.electric :as e]
    [hyperfiddle.electric-httpkit-adapter :as electric]
    [tesserae.eval]
    [tesserae.ui.app]
    ;; require order matters
    [ring.middleware.resource :refer [wrap-resource]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.not-modified :refer [wrap-not-modified]]))

(defn document
  [{:as opts :keys [js styles links meta body title]}]
  [:html
   [:head
    #_[:link {:rel "icon" :href "data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>🤘</text></svg>"}]
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
                :js    [{:src "/js/compiled/main.js"}
                        #_{:src "https://cdn.tailwindcss.com"}
                        #_{:src "/js/tailwind.js"}]
                :body  [:div#root]}])})

(defn demo-index [_]
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
                :js    [{:src "/js/compiled/demo/main.js"}
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
         (e/boot-server {} sst.ui.app/Main)
         #_tesserae.ui.app/Main))
     req)))

;; SEPERATE APPS??

(defn wrap-tesserae-electric-websocket [next-handler]
  (fn [req]
    ((electric/wrap-electric-websocket
       next-handler
       (fn boot [_req]
         (e/boot-server {} tesserae.ui.app/Main {:db/id 1})))
     req)))

(def router
  (r.ring/router
    [["/"
      {:get (fn [_] (resp/redirect "/talk"))}]
     ["/talk*"
      {:middleware [#_(wrap-auth-redirect-fn "/oauth/google")
                    #_wrap-electric-websocket]

       :get sst-index}]
     ["/demo"
      {:middleware [#_(wrap-auth-redirect-fn "/oauth/google")
                    #_wrap-tesserae-electric-websocket]

       :get sst-index}]
     ]))


(def handler
  (r.ring/ring-handler
    router
    (r.ring/create-default-handler)
    {:middleware [#_default-middleware
                  #(wrap-defaults %
                                  (-> site-defaults
                                      (assoc-in [:security :anti-forgery] false)
                                      (assoc-in [:session :cookie-attrs :same-site] :lax)))
                  ;wrap-user-session
                  #_(wrap-oauth2 % (oauth-config))
                  (fn [h]
                    #(h (su/dtap %)))
                  #_(fn [h]
                      #(h (do (tap> %)
                              %)))
                  ;wrap-electric-websocket
                  wrap-tesserae-electric-websocket
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

  #_(-> (mount/with-args
          {:tesserae.db/dir          "data/dash/datalevin/db"
           :tesserae.eval/namespaces {'ui.financials  'dash.ui.financials
                                      'ui.team        'dash.ui.team
                                      'ui.utilization 'dash.ui.utilization
                                      'ui.pricing     'dash.ui.pricing
                                      'harvest        'dash.plugin.harvest
                                      'invoice        'dash.plugin.invoice}
           :tesserae.eval/bindings   {}})
        (mount/start))
  (mount/start)
  (println "SpreadSheesh ready."))


(comment
  (do
    (when env/dev?
      (-> (mount/with-args
            {#_#_:tesserae.db/dir          "data/sst/tesserae/db"
             #_#_:tesserae.eval/namespaces {'ui.financials  'dash.ui.financials
                                        'ui.team        'dash.ui.team
                                        'ui.utilization 'dash.ui.utilization
                                        'ui.pricing     'dash.ui.pricing
                                        'harvest        'dash.plugin.harvest
                                        'invoice        'dash.plugin.invoice
                                        #_#_#_#_'openai 'dash.plugin.openai
                                                'zillow 'dash.plugin.zillow}
             :tesserae.eval/bindings   {}})
          (mount/start))
      #_(mount/start)
      (future
        (do
          (def shadow-start! @(requiring-resolve 'shadow.cljs.devtools.server/start!))
          (def shadow-stop! @(requiring-resolve 'shadow.cljs.devtools.server/stop!))
          (def shadow-watch @(requiring-resolve 'shadow.cljs.devtools.api/watch))
          (do
            (shadow-start!)
            (shadow-watch :sst)
            (shadow-watch :demo))))
      (comment
        (shadow-stop!)
        (def shadow-release @(requiring-resolve 'shadow.cljs.devtools.api/release))
        (shadow-release :sst))))
  )

