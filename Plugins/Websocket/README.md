# NitroWebsockets

## What is NitroWebsockets? ##
NitroWebsockets is a plugin that adds Nitro HTML5 Client compatibility to any hotel running Arcturus MS 3.0.0 and above.

## How do I configure the plugin?
Startup the plugin so it generates the required entries under your `emulator_settings` table. The following fields will be generated:
- `websockets.whitelist` - a comma-delimited list containing all permitted Origin headers. You should write the domain name of your hotel here, since the Websocket connection will be initiated there. Wildcards are also supported, so you can whitelist all subdomains by adding for example: `*.example.com`, do not use whitelist all origins by adding `*` (This will break your websocket due to monitoring)
- `ws.nitro.host` - host ip, should leave it as 0.0.0.0
- `ws.nitro.port` - host port, can be any port but if you want to proxy wss traffic with Cloudflare read the following section
- `ws.nitro.ip.header` - header that will be used for obtaining the user's real ip address if server is behind a proxy. Will most likely be needed to be set to `X-Forwarded-For` or `CF-Connecting-IP` if behind Cloudflare.

## How do I connect to my emulator using Secure Websockets (wss)? ##
You have several options to add WSS support to your websocket server. 

- You can add your certificate and key file to the path `/ssl/cert.pem` and `/ssl/privkey.pem` to add WSS support directly to the server **Note**:The client will not accept self-signed certificates, you must use a certificate signed by a CA (you can get one for free from letsencrypt.org)
  
- **RECOMMENDED** You can proxy WSS with either cloudflare or nginx. **Note**: Adding a proxy means that you will have to configure `ws.nitro.ip.header` so that the plugin is able to get the player's real ip address, and not the IP address of the proxy.

### Proxying WSS with Cloudflare
You can easily proxy wss traffic using Cloudflare. However, you should first make sure that your `ws.nitro.port` is set to one that is listed as HTTPS Cloudflare Compatible in the following link:
https://support.cloudflare.com/hc/en-us/articles/200169156-Which-ports-will-Cloudflare-work-with-

As of writing this, the following ports are listed as compatible:
- 443
- 2053
- 2083
- 2087
- 2096
- 8443

After your port is set to one that is compatible, create a new A record for a subdomain that will be used for websocket connections, and make sure that it is set to be proxied by Cloudflare (the cloud should be orange if it is being proxied). It should be pointing to your emulator IP.

Finally, create a new page rule under the Page Rules tab in Cloudflare and disable SSL for the subdomain you created above. You will now be able to connect using secure websockets using the following example url, where I created an A record for the subdomain `ws` and I set my `ws.nitro.port` to 2096: `wss://ws.example.com:2096` 

### Proxying WSS with Nginx
Alternatively, you can also proxy wss traffic with nginx. You will need a CA-signed certificate, since some browsers will block the connection on self-signed certificates. Below is an example nginx configuration file:

```
server {
    listen 80;
    listen 443 ssl http2;
    server_name ws.example.com;

    # Path for SSL config/key/certificate
    ssl_certificate /etc/ssl/certs/nginx/cert.pem;
    ssl_certificate_key /etc/ssl/certs/nginx/key.pem;
    ssl_protocols       TLSv1 TLSv1.1 TLSv1.2;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    location / {
      proxy_set_header        Host $host;
      proxy_set_header        X-Real-IP $remote_addr;
      proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header        X-Forwarded-Proto $scheme;


      proxy_pass          http://localhost:2096;
      proxy_read_timeout  90;


      # WebSocket support
      proxy_http_version 1.1;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection "upgrade";


    }
}
```
## FAQS ##
**I am getting the error `Unable to load ssl: File does not contain valid private key: ssl\privkey.pem`**

Make sure your private key is in PKCS#8 format. You can convert it to PKCS8 format with the following command:
```
openssl pkcs8 -topk8 -nocrypt -in yourkey.pem -out yournewkey.pem
```


**I am getting disconnected from the client with no error logs**

Make sure your sso ticket is valid and that you didn't do an IP ban before configuring the `ws.nitro.ip.header` if you're behind a proxy.

## License ##
This plugin is released under the GNU GPLv3
