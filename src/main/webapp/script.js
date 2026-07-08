let websocket;
let wsUri = sessionStorage.getItem("wsUriGlob")
const gameID = sessionStorage.getItem("gameId");
const username = sessionStorage.getItem("username")
const sessionId = sessionStorage.getItem("ssID");
let pwdHash = sessionStorage.getItem("pwdHash");
let user1;
let user2;
//game
// Websocket Erklärung: https://javascript.info/websocket
/**
 * Senden der Daten per Websocket
 * @param dataBlock = Daten die gesendet werden
 */
function sendData(dataBlock){
    const gameDAT=JSON.stringify(dataBlock);
    const combined=sessionId+gameDAT+pwdHash;
    const combinedhash= CryptoJS.SHA256(combined).toString();
    const transferData={
        sessionid: sessionId,
        data: gameDAT,
        hash: combinedhash
    }
    console.log(transferData);
    websocket.send(JSON.stringify(transferData));
}

/**
 * Testen ob eine Session für game vorhanden ist sonst zum start zurück
 */
async function loadProfileGame() {
    const lobbyData = {
        lobby: "get"
    }
    const logoutDAT = JSON.stringify(lobbyData);
    const combined = sessionId + logoutDAT + pwdHash;
    const comhash = CryptoJS.SHA256(combined).toString();
    try {
        const response= await fetch("api/lobby", {
            method: "GET",
            headers: {
                "sessionid": sessionId,
                "data": logoutDAT,
                "hash": comhash
            },
        });
        if (!response.ok) {
            sessionStorage.clear();
            window.location.href = "index.html"
        }

    } catch (error) {
        console.error(error);
    }
}

/**
 * Ping für das Game
 */
async function pingGame(){
    console.log("ping", websocket.readyState)
    if (websocket && websocket.readyState === WebSocket.OPEN) {
        const gameData={
            type: "PING",
            gameID: gameID
        }
        sendData(gameData);
        console.log("Ping gesendet");
    }
}

/**
 * Verbinden des Websockets für game + spielablauf
 */
function connectWebSocketGame() {
    websocket = new WebSocket(wsUri);
    const gameData={
        type: "GAME_CONNECT",
        username: username,
        gameID: gameID
    }
    websocket.onopen = () => {
        console.log("WebSocket verbunden");
        sendData(gameData);
        setTimeout(() => {
            pingGame();
            setInterval(pingGame, 2000);
        }, 100);
    };
    //Verarbeiten der antworten des Servers
    websocket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        console.log(message);
        //Aufbau des Spielfeldes
        if (message.type[0] === "USERNAMES") {
            let activePlayer = message.active_player[0]
            user1 = message.usernames[0];
            user2 = message.usernames[1];
            console.log("spieler 1=" + user1 + " spieler 2=" + user2);
            document.getElementById("player2").textContent = user2;
            document.getElementById("player1").textContent = user1;
            if (activePlayer === user1) {
                document.getElementById("player-1").style.backgroundColor = "red";
            } else {
                document.getElementById("player-2").style.backgroundColor = "gold"
            }
            updateGrid(message.matrix);
            //anzeigen der PUT-Buttons(blau=man ist am Zug/grau=gegner ist am Zug)
            if (activePlayer === username) {
                for (let i = 0; i < 7; i++) {
                    const col = document.getElementById(`col-${i}`);
                    col.style.backgroundColor = "#00f"
                }
            } else {
                for (let i = 0; i < 7; i++) {
                    const col = document.getElementById(`col-${i}`);
                    col.style.backgroundColor = "#aaa"
                }
            }
            //Anpassen der Münze unter Spielernamen
        } else if (message.type[0] === "GAME_TURN") {
            let activePlayer = message.active_player[0]
            updateGrid(message.matrix);
            if (user1 === activePlayer) {
                document.getElementById("player-1").style.backgroundColor = "red";
                document.getElementById("player-2").style.backgroundColor = "white";
            } else {
                document.getElementById("player-1").style.backgroundColor = "white";
                document.getElementById("player-2").style.backgroundColor = "gold";
            }
            //Auswerten des Spiels
            if (message.game_state[0] === "DRAW") {
                showMessage("Unentschieden",null);
                setTimeout(() => {
                    sessionStorage.setItem("searchBlock","false");
                    window.location.href = "lobby.html";
                }, 3000);
            } else if (message.game_state[0] === "USER1WON") {
                showMessage(user1 + " hat gewonnen",user1);
                setTimeout(() => {
                    sessionStorage.setItem("searchBlock","false");
                    window.location.href = "lobby.html";
                }, 3000);
            } else if (message.game_state[0] === "USER2WON") {
                showMessage(user2 + " hat gewonnen",user2);
                setTimeout(() => {
                    sessionStorage.setItem("searchBlock","false");
                    window.location.href = "lobby.html";
                }, 3000);
            }
            //Button farbe
            if (activePlayer === username) {
                for (let i = 0; i < 7; i++) {
                    const col = document.getElementById(`col-${i}`);
                    col.style.backgroundColor = "#00f"
                }
            } else {
                for (let i = 0; i < 7; i++) {
                    const col = document.getElementById(`col-${i}`);
                    col.style.backgroundColor = "#aaa"
                }
            }
            //Aufgeben auswerten
        } else if (message.type[0] === "GIVE_UP") {
            if (message.game_state[0] === "USER1WON") {
                showMessage(user2 + " hat aufgegeben. " + user1 + " hat gewonnen!",user1)
                setTimeout(() => {
                    sessionStorage.setItem("searchBlock","false");
                    window.location.href = "lobby.html";
                }, 3000);
            } else if (message.game_state[0] === "USER2WON") {
                showMessage(user1 + " hat aufgegeben. " + user2 + " hat gewonnen!",user2)
                setTimeout(() => {
                    sessionStorage.setItem("searchBlock","false");
                    window.location.href = "lobby.html";
                }, 3000);
            }
        }
    };
    websocket.onclose=()=>{};
}

/**
 * Ausgabe des Entergebnisses
 * @param text = Nachricht
 * @param winner = Für farbe des Randes
 */
function showMessage(text,winner) {
    const box = document.getElementById("gameMessage");
    disableInput();
    box.textContent = text;
    box.classList.remove("hidden");
    if (winner === user1) {
        box.style.borderColor = "red";
    } else if(winner===user2){
        box.style.borderColor = "gold";
    }else{
        box.style.borderColor = "black";
    }
}

/**
 * Spielfeld aktualisieren
 * @param matrix = Spielfeld
 */
function updateGrid(matrix) {
    for (let i = 0; i < matrix.length; i++) {

        const cell = document.getElementById(`cell-${i}`);

        cell.style.backgroundColor = "white";

        if (matrix[i] === "RED") {
            cell.style.backgroundColor = "red";
        }

        if (matrix[i] === "YELLOW") {
            cell.style.backgroundColor = "gold";
        }
    }

}
/**
* Buttons deaktivieren
*/
function disableInput(){
    for (let i=0;i<7;i++){
        const button=document.getElementById(`col-${i}`);
        button.disabled=true;
    }
}

/**
 * Senden einer Spalte
 * @param index = Spalte
 */
function sendCol(index) {
    if (websocket.readyState !== WebSocket.OPEN) {
        alert("Keine Verbindung zum Server");
        return;
    }
    const gameData={
        type: "GAME_TURN",
        column: index,
        gameID: gameID
    }
    sendData(gameData)
}
function aufgeben() {
    if (websocket.readyState !== WebSocket.OPEN) {
        alert("Keine Verbindung zum Server");
        return;
    }
    const gameData={
        type: "GIVE_UP",
        gameID: gameID
    }
    sendData(gameData)

}

//lobby
let pusername;

/**
 * Aufbau der Lobby Seite
 */
async function loadProfile() {
    const lobbyData = {
        lobby: "get"
    }
    const logoutDAT = JSON.stringify(lobbyData);
    const combined = sessionId + logoutDAT + pwdHash;
    const comhash = CryptoJS.SHA256(combined).toString();
    try {
        const response = await fetch("api/lobby", {
            method: "GET",
            headers: {
                "sessionid": sessionId,
                "data": logoutDAT,
                "hash": comhash
            },
        });

        if (!response.ok) {
            sessionStorage.clear();
            window.location.href = "index.html"
        }

        const profile = await response.json();
        console.log(profile);
        pusername=profile.username;
        document.getElementById("username").textContent = "Willkommen, " + pusername;
        document.getElementById("wins").textContent = profile.wins;
        document.getElementById("draws").textContent = profile.draws;
        document.getElementById("losses").textContent = profile.losses;
        if(sessionStorage.getItem("searchBlock")==="true"){
            document.getElementById("search").value = "Suche läuft...";
            document.getElementById("search").disabled = true;
        }

    } catch (error) {
        console.error(error);
    }
}

/**
 * ping für Lobby
 */
async function pingLobby(){
    console.log("ping", websocket.readyState)
    if (websocket && websocket.readyState === WebSocket.OPEN) {
        const gameData={
            type: "PING",
            gameID: -1
        }
        sendData(gameData);
        console.log("Ping gesendet");
    }
}

/**
 * Websocket für Lobby
 */
function connectWebSocketLobby(){
    websocket = new WebSocket(wsUri);
    const lobbyData={
        type: "LOBBY_CONNECT",
        username: pusername
    };

    websocket.onopen = () => {
        console.log("WebSocket verbunden");
        sendData(lobbyData);
        setTimeout(() => {
            pingLobby();
            setInterval(pingLobby, 2000);
        }, 100);
    };
    //weiterleiten an game wenn ein Spiel gefunden wurde
    websocket.onmessage = (event) => {
        console.log("Server:", event.data);
        const message = JSON.parse(event.data);
        if(message.type === "GAME_FOUND"){
            sessionStorage.setItem("gameId", message.gameID);
            sessionStorage.setItem("username", pusername);
            window.location.href = "game.html";
        }
    };
    websocket.onerror = (error) => {console.error("WebSocket Fehler", error);};
    websocket.onclose=()=>{

    };
}

/**
 * Suche nach einem Spiel anfragen
 */
function search(){
    if(websocket.readyState !== WebSocket.OPEN){
        alert("Keine Verbindung zum Server");
        return;
    }
    const lobbyData={
        type: "SEARCH_GAME",
    };
    sendData(lobbyData);
    sessionStorage.setItem("searchBlock","true")
    document.getElementById("search").value = "Suche läuft...";
    document.getElementById("search").disabled = true;
}

/**
 * ausloggen des Benutzers + zurücksenden zuum Start
 */
async function logout(){
    const logoutData={
        logout: "abmelden"
    }
    const logoutDAT= JSON.stringify(logoutData);
    const combined=sessionId+logoutDAT+pwdHash;
    const comhash=CryptoJS.SHA256(combined).toString();
    const transferData={
        sessionid: sessionId,
        data: logoutDAT,
        hash:comhash
    }
    const response = await fetch("api/account/logout", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(transferData)
    });
    try{
        if(response.ok){
            console.log("logout: "+pusername)
            sessionStorage.clear();
            window.location.href="index.html";
        } else {
            const error =  await response.text();
            console.log(error)
            console.error(error);
        }
    } catch(error) {
        console.error(error);
        alert("Server nicht erreichbar");
    }
}
//index
let seseinId;

/**
 * erhalten  der SessionID und der IP-Adresse des Servers für websocket aufbau
 */
async function getSession() {
    try {
        const response = await fetch("api/sessionid");
        console.log(response)
        const session = await response.json();
        console.log(session);
        seseinId = session.SESSIONID;
        console.log(seseinId);
        const wsUriTemp="ws://"+session.ADDRESS+":8080/VS_Projekt_vierGewinnt/game"
        sessionStorage.setItem("wsUriGlob", wsUriTemp);
    } catch (error) {
        console.error(error);
    }
}

/**
 * anmelden mit einem Account
 */
async function login() {
    const username = document.getElementById("name").value;
    const password = document.getElementById("passwort").value;
    if(username.trim() === "") {
        alert("Bitte Username eingeben");
        return;
    }
    if(password.trim() === "") {
        alert("Bitte Passwort eingeben");
        return;
    }
    const hash = CryptoJS.SHA256(password).toString();
    console.log(hash);
    const loginData = {
        username: username
    };
    const loginDAT=JSON.stringify(loginData);
    const combined = seseinId+loginDAT+hash;
    console.log(combined);
    const combinedhash = CryptoJS.SHA256(combined).toString();
    console.log(combinedhash)
    const tranferData={
        sessionid: seseinId,
        data: loginDAT,
        hash: combinedhash
    }
    console.log(tranferData)
    try {
        const response = await fetch("api/account/login", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(tranferData)
        });
        if(response.ok) {
            sessionStorage.setItem("ssID", seseinId);
            sessionStorage.setItem("pwdHash", hash);
            window.location.href = "lobby.html";
        } else {
            const error = await response.text();
            alert(error);
        }
    } catch(error) {
        console.error(error);
        alert("Server nicht erreichbar");
    }
}

//register
/**
 * Benutzer registrieren
 */
async function register() {
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
    const passwordRepeat = document.getElementById("passwordRepeat").value;
    // Eingaben prüfen
    if (username.trim() === "") {
        alert("Bitte Benutzernamen eingeben");
        return;
    }
    if (password.trim() === "") {
        alert("Bitte Passwort eingeben");
        return;
    }
    if (password !== passwordRepeat) {
        alert("Passwörter stimmen nicht überein");
        return;
    }
    //Verschlüsselung
    const response = await fetch("api/publickey");
    const passwordHash= CryptoJS.SHA256(password).toString();
    const publicKey = await response.text();

    const encryptor = new JSEncrypt();

    encryptor.setPublicKey(publicKey);

    const userDAT = JSON.stringify({
        username: username,
        passwordHash: passwordHash
    });

    const encrypted = encryptor.encrypt(userDAT);
    console.log(encrypted);
    try {
        const response = await fetch("api/account/register", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(encrypted)
        });
        if (response.ok) {
            sessionStorage.setItem("ssID", seseinId);
            sessionStorage.setItem("pwdHash", passwordHash);
            window.location.href = "lobby.html";
        } else {
            const perror = await response.text();
            alert("Registrierung fehlgeschlagen: " + perror);
        }
    } catch (error) {
        console.error("Fehler:", error);
        alert("Server nicht erreichbar");
    }
}