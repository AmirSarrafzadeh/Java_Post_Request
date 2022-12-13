package com.string.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.ini4j.Ini;
import org.json.simple.parser.ParseException;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.net.HttpURLConnection;

import static org.apache.commons.codec.binary.Base64.*;

public class App{
    private static Ini aRead;
    private static String user, password, map_url, token_url;
    private static String field_name, token, att_name;
    private static HttpURLConnection connection1, connection2;
    private static Integer table_id;
    private static String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }
        return result.toString();
    }

    public static void main(String[] args) throws MalformedURLException, JsonProcessingException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        // Read the Config file
        aRead = new Ini();
        String config_path = System.getProperty("user.dir");
        try {
            aRead.load(new FileReader(config_path + "/config.ini"));
            System.out.println("The config.ini file is read from the " + config_path + " directory");
        } catch (Exception e) {
            System.out.println("The config.ini file is not in the directory: " + config_path);
        }

        try {
            user = aRead.get("config", "username", String.class);
            password = aRead.get("config", "password", String.class);
            token_url = aRead.get("config", "token_url", String.class);
            map_url = aRead.get("config", "map_url", String.class);
            table_id = aRead.get("config", "table_id", Integer.class);
            field_name = aRead.get("config", "field_name", String.class);
            att_name = aRead.get("config", "att_name", String.class);

        } catch (Exception e) {
            System.out.println("There is error " + e + " for reading Config file in directory: " + config_path);
            e.printStackTrace();
        }

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        System.out.println("All the certificates are put in the trust manager");

        // Skip all the certificates
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            System.out.println("All certificates which is necessary for reaching to the url are skipped");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("There is an error in skip Certification " + e);
        }
        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);

        URL get_token = new URL(token_url);

        StringBuilder content1;
        try {
            connection1 = (HttpURLConnection) get_token.openConnection();
            connection1.setRequestMethod("POST");
            String auth = user + ":" + password;
            byte[] encodedAuth = encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
            String authHeaderValue = "Basic " + new String(encodedAuth);
            connection1.setRequestProperty("Authorization", authHeaderValue);
            connection1.setDoInput(true);
            connection1.setDoOutput(true);
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("username", user));
            params.add(new BasicNameValuePair("password", password));
            params.add(new BasicNameValuePair("referer", map_url));
            params.add(new BasicNameValuePair("f", "json"));
            OutputStream os = connection1.getOutputStream();
            //create a writer object
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getQuery(params));
            writer.flush();
            writer.close();
            os.close();
            connection1.connect();
            int responseCode = connection1.getResponseCode();
            System.out.println("The response code is " + responseCode);
//            Authenticator.setDefault(new App());
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection1.getInputStream()))) {
                String line;
                content1 = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    content1.append(line);
                    content1.append(System.lineSeparator());
                }
            }
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            connection1.disconnect();
        }
        ObjectMapper mapper1 = new ObjectMapper();
        JsonNode jsonNode1 = mapper1.readTree(content1.toString());
        token = String.valueOf(jsonNode1.get("token"));
        System.out.println("This is the token: " + token.substring(1,token.length()-1));

        map_url += table_id + "/query";

        String url_1 = map_url + "?token=" + token.substring(1,token.length()-1) + "&where=1=1&f=json&outFields=*";
        System.out.println(url_1);
        URL url = new URL(url_1);
        StringBuilder content2;
        try {
            connection2 = (HttpURLConnection) url.openConnection();
            connection2.setRequestMethod("GET");
            connection2.setConnectTimeout(5000);
            connection2.setReadTimeout(5000);
            connection2.setDoInput(true);
            connection2.setDoOutput(true);

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("token", token.substring(1,token.length()-1)));
            params.add(new BasicNameValuePair("where", "1=1"));
            params.add(new BasicNameValuePair("f", "json"));
            params.add(new BasicNameValuePair("outFields", "*"));
            //open the connection
            OutputStream os = connection2.getOutputStream();
            //create a writer object
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getQuery(params));
            writer.flush();
            writer.close();
            os.close();
            connection2.connect();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection2.getInputStream()))) {
                String line;
                content2 = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    content2.append(line);
                    content2.append(System.lineSeparator());
                }
            }
            System.out.println(content2);
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            connection2.disconnect();
        }

        ObjectMapper mapper2 = new ObjectMapper();
        JsonNode jsonNode2 = mapper2.readTree(content2.toString());
        HashMap <String, String> id_list = new HashMap<>();
        for(int i = 0; i < jsonNode2.get("features").size(); i++){
            String id_ambito = jsonNode2.get("features").get(i).get("attributes").get("ID_Ambito").toString();
            String id_group = jsonNode2.get("features").get(i).get("attributes").get("ID_Group").toString();
            id_list.put(id_ambito, id_group);
        }
        System.out.println(id_list);
    }
}
