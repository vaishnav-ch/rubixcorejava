package com.rubix.Ping;

import io.ipfs.api.IPFS;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

import static com.rubix.Resources.Functions.*;
import static com.rubix.Resources.IPFSNetwork.*;
import static com.rubix.Resources.IPFSNetwork.executeIPFSCommands;

public class PingCheck {
    private static final Logger PingSenderLogger = Logger.getLogger(PingCheck.class);
    public static IPFS ipfs = new IPFS("/ip4/127.0.0.1/tcp/" + IPFS_PORT);

    public static JSONObject Ping(String peerID,  int port) throws IOException, JSONException {
        repo(ipfs);
        PropertyConfigurator.configure(LOGGER_PATH + "log4jWallet.properties");

        JSONObject APIResponse = new JSONObject();
        if (!peerID.equals("")) {
            PingSenderLogger.debug("Swarm connecting to " + peerID);
            swarmConnectP2P(peerID, ipfs);
            PingSenderLogger.debug("Swarm connected");
        } else {
            APIResponse.put("message", "Receiver Peer ID null");
            PingSenderLogger.warn("Receiver Peer ID null");
            return APIResponse;
        }

        String receiverWidIpfsHash = getValues(DATA_PATH + "DataTable.json", "walletHash", "peerid", peerID);
        String receiverDidIpfsHash = getValues(DATA_PATH + "DataTable.json", "didHash", "peerid", peerID);
        if (!receiverWidIpfsHash.equals("")) {
            nodeData(receiverDidIpfsHash, receiverWidIpfsHash, ipfs);
        } else {
            APIResponse.put("status", "Failed");
            APIResponse.put("message", "Receiver WID null");
            PingSenderLogger.warn("Receiver WID null");
            return APIResponse;
        }

        PingSenderLogger.debug("Sender IPFS forwarding to DID: " + receiverDidIpfsHash + " PeerID: " + peerID);
        String appName = peerID.concat("Ping");
        forward(appName, port, peerID);
        PingSenderLogger.debug("Forwarded to " + appName + " on " + port);
        Socket senderSocket = new Socket("127.0.0.1", port);

        BufferedReader input = new BufferedReader(new InputStreamReader(senderSocket.getInputStream()));
        PrintStream output = new PrintStream(senderSocket.getOutputStream());

        output.println("PingCheck");
        PingSenderLogger.debug("Sent PingCheck request");

        String pongResponse;
        try {
            pongResponse = input.readLine();
        } catch (SocketException e) {
            PingSenderLogger.warn("Receiver " + receiverDidIpfsHash + " is unable to Respond! - Ping Check");
            executeIPFSCommands(" ipfs p2p close -t /p2p/" + peerID);
            output.close();
            input.close();
            senderSocket.close();
            APIResponse.put("status", "Failed");
            APIResponse.put("message", "Receiver " + receiverDidIpfsHash + "is unable to respond! - Sender Auth");

            return APIResponse;
        }


        if (pongResponse != null && (!pongResponse.equals("Pong"))) {
            executeIPFSCommands(" ipfs p2p close -t /p2p/" + peerID);
            PingSenderLogger.info("Pong response not received");
            output.close();
            input.close();
            senderSocket.close();
            APIResponse.put("status", "Failed");
            APIResponse.put("message", "Pong response not received");

        }else {
            PingSenderLogger.info("Ping Successful");
            executeIPFSCommands(" ipfs p2p close -t /p2p/" + peerID);
            output.close();
            input.close();
            senderSocket.close();
            APIResponse.put("status", "Success");
            APIResponse.put("message", "Ping Check Success");

        }
        return APIResponse;
    }
}
