package de.hsos.vs.viergewinnt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hsos.vs.viergewinnt.data.ServerData;
import de.hsos.vs.viergewinnt.data.userData.Account;
import de.hsos.vs.viergewinnt.data.userData.PublicData;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@WebServlet("/api/*")
public class VierGewinntServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ServerData serverData = ServerData.getInstance();




    @Override
    public void init() {
        serverData.init();
        System.out.println("--- Servlet läuft ---\n");
    }



    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        System.out.println("\nGET erhalten...");

        String pathInfo = request.getPathInfo();
        HttpSession session = request.getSession();
        String sessionID = session.getId();


        // Client fragt Statistiken an
        if (pathInfo != null && pathInfo.equals("/lobby")) {
            System.out.println("LOBBY...");

            Map<String, String> data = new HashMap<>();
            data.put("sessionid", request.getHeader("sessionid"));
            data.put("data",request.getHeader("data"));
            data.put("hash",request.getHeader("hash"));

            // Daten holen
            Map<?, ?> validatedData = objectMapper.readValue(serverData.getData(data), Map.class); // data != null wenn Validierung erfolgreich

            if(validatedData == null) {
                // Antwort an Client senden
                response.getWriter().write("Validierung nicht erfolgreich");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            } else {
                // Account ermitteln
                String username = serverData.getUsernameFromSession(sessionID);
                if (username == null) {
                    response.getWriter().write("Ungültige Session");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                Account account = serverData.getAccount(username);

                // Benutzerdaten an Client senden
                PublicData publicData = account.getPublicData();
                String publicDataJson = objectMapper.writeValueAsString(publicData);
                response.getWriter().write(publicDataJson);
            }


        } else if (pathInfo != null && pathInfo.equals("/sessionid")) {
            System.out.println("SESSIONID...");

            // SessionID senden
            Map<String, String> sessionData = new HashMap<>();
            sessionData.put("SESSIONID", sessionID);
            String ipAddress = serverData.getIPAddress();
            sessionData.put("ADDRESS", ipAddress);
            String sessionIDJson = objectMapper.writeValueAsString(sessionData);
            response.getWriter().write(sessionIDJson);
            response.setStatus(HttpServletResponse.SC_OK);




        } else if (pathInfo != null && pathInfo.equals("/publickey")) {
            System.out.println("PUBLICKEY...");
            String publicKey = serverData.getPublicKey();
            response.getWriter().write(publicKey);
            response.setStatus(HttpServletResponse.SC_OK);



        // keine gültige Rest-Adresse
        } else {
            // Antwort an Client senden
            response.getWriter().write("Unbekannte REST-Adresse");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }


    // POST: Neue Matrix-Daten vom Client empfangen und speichern
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        System.out.println("\nPOST erhalten...");

        HttpSession session = request.getSession();
        String sessionID = session.getId();
        String pathInfo = request.getPathInfo();


        // Client möchte neues Konto erstellen
        if (pathInfo != null && pathInfo.equals("/account/register")) {
            System.out.println("REGISTER...");

            // Daten holen
            String encryptedData  = request.getReader().lines().collect(Collectors.joining());
            // Nachricht entschlüsseln
            String decryptedData = serverData.decryptMessage(encryptedData);

            Map<?, ?> decryptedDataMap = objectMapper.readValue(decryptedData, Map.class);
            String username = decryptedDataMap.get("username").toString();
            String passwordHash = decryptedDataMap.get("passwordHash").toString();



            // Daten auf null prüfen
            if (username == null || passwordHash == null) {
                // Antwort an Client senden
                response.getWriter().write("Eingabe falsch formatiert");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (!serverData.accountExists(username)) { // username noch nicht vergeben
                // Account erstellen
                Account account = new Account(username,passwordHash);

                // Account speichern
                serverData.putAccount(username, account);
                serverData.loginAccount(sessionID, username);
                response.setStatus(HttpServletResponse.SC_OK);
            }

            else { // username bereits vergeben
                // Antwort an Client senden
                response.getWriter().write("Username bereits vergeben");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        }





        // Client möchte sich anmelden
        else if (pathInfo != null && pathInfo.equals("/account/login")) {
            System.out.println("LOGIN...");
            // Daten holen
            Map<?, ?> data = objectMapper.readValue(request.getInputStream(), Map.class);
            String dataString =  serverData.getData(data); // data != null wenn Validierung erfolgreich

            if (dataString  == null){
                // Antwort an Client senden
                response.getWriter().write("Validierung nicht erfolgreich/Username nicht vergeben");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            } else {
                Map<?, ?> validatedData = objectMapper.readValue(dataString, Map.class);
                String username = validatedData.get("username").toString();

                // Daten auf null prüfen
                if (username == null) {
                    // Antwort an Client senden
                    response.getWriter().write("Eingabe falsch formatiert");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                if (!serverData.accountExists(username)) {
                    // Antwort an Client senden
                    response.getWriter().write("Username nicht vergeben");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

                } else if (serverData.isAccountsLoggedIn(username)) {
                    // Antwort an Client senden
                    response.getWriter().write("Account ist bereits angemeldet");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                } else {
                    // Anmedlung durchführen
                    serverData.loginAccount(sessionID, username);
                    System.out.println("Angemeldet: " + username);
                    response.setStatus(HttpServletResponse.SC_OK);
                }
            }
        }



        else if (pathInfo != null && pathInfo.equals("/account/logout")) {
            System.out.println("LOGOUT...");
            // Daten holen
            Map<?, ?> data = objectMapper.readValue(request.getInputStream(), Map.class);
            Map<?, ?> validatedData = objectMapper.readValue(serverData.getData(data), Map.class); // data != null wenn Validierung erfolgreich

            System.out.println(data);
            System.out.println(validatedData);


            if (validatedData == null) {
                // Antwort an Client senden
                response.getWriter().write("Validierung nicht erfolgreich");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            } else {
                String username = serverData.getUsernameFromSession(sessionID);
                serverData.logoutAccount(username);
                System.out.println("Abgemeldet: " + username);

                response.getWriter().write("Abmeldung erfolgreich");
                response.setStatus(HttpServletResponse.SC_OK);
            }
        }


        // keine gültige Rest-Adresse
        else {
            // Antwort an Client senden
            response.getWriter().write("Unbekannte REST-Adresse");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }


    





    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
           {
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
           {
    }

    @Override
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            {
    }

    @Override
    public void destroy() {
        serverData.shutdown();
    }
}
