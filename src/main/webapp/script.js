let websocket;
//game
const gameID = sessionStorage.getItem("gameId");
const username = sessionStorage.getItem("username")
const sessionId = sessionStorage.getItem("ssID");
let pwdHash = sessionStorage.getItem("pwdHash");
let user1;
let user2;
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
async function pingGame(){
    console.log("ping", websocket.readyState)
    if (websocket && websocket.readyState === WebSocket.OPEN) {
        const gameData={
            type: "PING",
            gameID: gameID
        }
        const gameDAT=JSON.stringify(gameData);
        const combined=sessionId+gameDAT+pwdHash;
        const combinedhash= CryptoJS.SHA256(combined).toString();
        const transferData={
            sessionid: sessionId,
            data: gameDAT,
            hash: combinedhash
        }
        console.log(transferData);
        websocket.send(JSON.stringify(transferData));
        console.log("Ping gesendet");
    }
}
function connectWebSocketGame() {
    websocket = new WebSocket(
        "ws://131.173.110.10:8080/VS_Projekt_vierGewinnt/game"
    );
    const gameData={
        type: "GAME_CONNECT",
        username: username,
        gameID: gameID
    }
    const gameDAT=JSON.stringify(gameData);
    const combined=sessionId+gameDAT+pwdHash;
    const combinedhash= CryptoJS.SHA256(combined).toString();
    const transferData={
        sessionid: sessionId,
        data: gameDAT,
        hash: combinedhash
    }
    console.log(transferData);
    websocket.onopen = () => {
        console.log("WebSocket verbunden");
        websocket.send(JSON.stringify(transferData));
        setTimeout(() => {
            pingGame();
            setInterval(pingGame, 2000);
        }, 100);
    };

    websocket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        console.log(message);
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
function disableInput(){
    for (let i=0;i<7;i++){
        const button=document.getElementById(`col-${i}`);
        button.disabled=true;
    }
}
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
    const gameDAT=JSON.stringify(gameData);
    const combined=sessionId+gameDAT+pwdHash;
    const combinedhash= CryptoJS.SHA256(combined).toString();
    const transferData={
        sessionid: sessionId,
        data: gameDAT,
        hash: combinedhash
    }
    console.log(transferData);
    websocket.send(JSON.stringify(transferData));
}function aufgeben() {
    if (websocket.readyState !== WebSocket.OPEN) {
        alert("Keine Verbindung zum Server");
        return;
    }
    const gameData={
        type: "GIVE_UP",
        gameID: gameID
    }
    const gameDAT=JSON.stringify(gameData);
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

//lobby
let pusername;
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
async function pingLobby(){
    console.log("ping", websocket.readyState)
    if (websocket && websocket.readyState === WebSocket.OPEN) {
        const gameData={
            type: "PING",
            gameID: -1
        }
        const gameDAT=JSON.stringify(gameData);
        const combined=sessionId+gameDAT+pwdHash;
        const combinedhash= CryptoJS.SHA256(combined).toString();
        const transferData={
            sessionid: sessionId,
            data: gameDAT,
            hash: combinedhash
        }
        console.log(transferData);
        websocket.send(JSON.stringify(transferData));
        console.log("Ping gesendet");
    }
}
function connectWebSocketLobby(){
    const wsUri = "ws://131.173.110.10:8080/VS_Projekt_vierGewinnt/game";
    websocket = new WebSocket(wsUri);
    const lobbyData={
        type: "LOBBY_CONNECT",
        username: pusername
    };
    const lobDAT=JSON.stringify(lobbyData);
    const combined=sessionId+lobDAT+pwdHash;
    const combinedhash= CryptoJS.SHA256(combined).toString();
    const transferData={
        sessionid: sessionId,
        data: lobDAT,
        hash: combinedhash
    }
    console.log(transferData);
    websocket.onopen = () => {console.log("WebSocket verbunden");
        websocket.send(JSON.stringify(transferData));
        setTimeout(() => {
            pingLobby();
            setInterval(pingLobby, 2000);
        }, 100);
    };
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
function search(){
    if(websocket.readyState !== WebSocket.OPEN){
        alert("Keine Verbindung zum Server");
        return;
    }
    const lobbyData={
        type: "SEARCH_GAME",
    };
    const lobDAT=JSON.stringify(lobbyData);
    const combined=sessionId+lobDAT+pwdHash;
    const combinedhash= CryptoJS.SHA256(combined).toString();
    const transferData={
        sessionid: sessionId,
        data: lobDAT,
        hash: combinedhash
    }
    console.log(transferData);
    // Nachricht an Server senden
    websocket.send(JSON.stringify(transferData));
    sessionStorage.setItem("searchBlock","true")
    document.getElementById("search").value = "Suche läuft...";
    document.getElementById("search").disabled = true;
}
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
async function getSession() {
    try {
        const response = await fetch("api/sessionid");
        console.log(response)
        const session = await response.json();
        console.log(session);
        seseinId = session.SESSIONID;
        console.log(seseinId);
    } catch (error) {
        console.error(error);
    }
}
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