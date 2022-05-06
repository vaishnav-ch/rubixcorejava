package com.rubix.NFT;

import com.rubix.AuthenticateNode.Authenticate;
import com.rubix.AuthenticateNode.PropImage;
import com.rubix.Resources.Functions;
import com.rubix.Resources.IPFSNetwork;
import io.ipfs.api.IPFS;
import io.ipfs.api.IPFS.Repo;

import static com.rubix.Constants.MiningConstants.*;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.imageio.ImageIO;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;

import static com.rubix.NFTResources.EnableNft.*;
import static com.rubix.Resources.Functions.*;
import com.rubix.NFTResources.NFTFunctions;
import com.rubix.PasswordMasking.PasswordField;
import com.rubix.Ping.VerifyStakedToken;

import static com.rubix.NFTResources.NFTFunctions.*;

public class NftSeller {
    public static Logger nftSellerLogger = Logger.getLogger(NftSeller.class);

    private static final JSONObject APIResponse = new JSONObject();

    private static final IPFS ipfs = new IPFS("/ip4/127.0.0.1/tcp/" + Functions.IPFS_PORT);

    // token limit for each level
    private static final int[] tokenLimit = { 0, 5000000, 2425000, 2303750, 2188563, 2079134, 1975178, 1876419, 1782598,
        1693468, 1608795, 1528355, 1451937, 1379340 };

    // public static JSONObject APIResponse = new JSONObject();
    public static String receive() {

        Functions.pathSet();
        nftPathSet();
        ServerSocket ss = null;
        Socket sk = null;
        String buyerPeerID = null;
        String PART_TOKEN_CHAIN_PATH = TOKENCHAIN_PATH.concat("PARTS/");
        String PART_TOKEN_PATH = TOKENS_PATH.concat("PARTS/");

        BufferedReader sysInput = new BufferedReader(new InputStreamReader(System.in));

        try {
            PropertyConfigurator.configure(LOGGER_PATH + "log4jWallet.properties");
            int quorumSignVerifyCount = 0;
            JSONObject quorumSignatures = null;
            ArrayList<String> quorumDID = new ArrayList<>();
            String sellerPeerID = Functions.getPeerID(Functions.DATA_PATH + "DID.json");
            String sellerDidIpfsHash = Functions.getValues(Functions.DATA_PATH + "DataTable.json", "didHash", "peerid",
                    sellerPeerID);
            IPFSNetwork.listen(sellerPeerID + "NFT", SELLER_PORT);
            ss = new ServerSocket(SELLER_PORT);
            nftSellerLogger.debug("Listening on " + SELLER_PORT + "with app name " + sellerPeerID + "NFT");
            sk = ss.accept();
            BufferedReader input = new BufferedReader(new InputStreamReader(sk.getInputStream()));
            PrintStream output = new PrintStream(sk.getOutputStream());
            long startTime = System.currentTimeMillis();

            try {
                buyerPeerID = input.readLine();
            } catch (SocketException e) {
                nftSellerLogger.warn("Buyer Stream Null - Buyer Details");
                APIResponse.put("did", "");
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "Buyer Stream Null - Buyer Detail");

                 
                output.close();
                input.close();
                sk.close();
                ss.close();
                return APIResponse.toString();
            }
            nftSellerLogger.debug("Buyer PeerID received");
            String buyerDidIpfsHash = Functions.getValues(Functions.DATA_PATH + "DataTable.json", "didHash", "peerid",
                    buyerPeerID);
            String buyerWidIpfsHash = Functions.getValues(Functions.DATA_PATH + "DataTable.json", "walletHash",
                    "peerid", buyerPeerID);
            if (!buyerDidIpfsHash.contains("Qm") || !buyerWidIpfsHash.contains("Qm")) {
                output.println("420");
                APIResponse.put("did", buyerDidIpfsHash);
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "Invalid Buyer DID.");
                nftSellerLogger.info("Buyer details not available in datatable");
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();
            }
            nodeData(buyerDidIpfsHash, buyerWidIpfsHash, ipfs);
            File buyerDIDFile = new File(Functions.DATA_PATH + buyerDidIpfsHash + "/DID.png");
            if (!buyerDIDFile.exists()) {
                output.println("420");
                APIResponse.put("did", buyerDidIpfsHash);
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "Buyer details not available in network , please sync");
                nftSellerLogger.info("Buyer details not available");
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();
            }
            output.println("200");
            nftSellerLogger.debug("PeerAuth sent to Seller");

            String nftTokenIpfsHash;

            try {
                nftTokenIpfsHash = input.readLine();
            } catch (SocketException e) {
                nftSellerLogger.warn("Buyer Stream Null - Buyer Details");
                APIResponse.put("did", "");
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "Buyer Stream Null - Buyer Detail");

                 
                output.close();
                input.close();
                sk.close();
                ss.close();
                return APIResponse.toString();
            }

            nftSellerLogger.debug("Seller recived the nft token for sale : "+ nftTokenIpfsHash);
            nftSellerLogger.debug("Seller Checking if he has NFT token : "+ nftTokenIpfsHash);
            File nfttoken = new File(NFT_TOKENS_PATH + nftTokenIpfsHash);
            File nfttokenchain = new File(NFT_TOKENCHAIN_PATH + nftTokenIpfsHash + ".json");

            // nftSellerLogger.debug("Seller Checking if he has NFT token");
            if (!nfttoken.exists() || !nfttokenchain.exists()) {
                output.println("420");
                nftSellerLogger.debug("NFT Token " + nftTokenIpfsHash + " and NFT Token chain " + nftTokenIpfsHash
                        + ".json files does not exist");
                APIResponse.put("message", "NFT Token " + nftTokenIpfsHash + " and NFT Token chain " + nftTokenIpfsHash
                        + ".json files does not exist");
                APIResponse.put("did", sellerDidIpfsHash);
                APIResponse.put("tid", "");
                APIResponse.put("status", "Failed");
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();
            }
            nftSellerLogger.debug("Seller has nft token "+nftTokenIpfsHash);
            output.println("200");

            String p2pFlagstr;
            try {
                p2pFlagstr = input.readLine();
            } catch (SocketException e) {
                nftSellerLogger.warn("Buyer Stream Null - Buyer Details");
                APIResponse.put("did", "");
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "Buyer Stream Null - Buyer Detail");

                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();
            }

            int p2pFlag = Integer.parseInt(p2pFlagstr);

            if (p2pFlag == 1) {
                nftSellerLogger.debug("this is a Peer to Peer NFT Transaction");
                nftSellerLogger.info("Please enter the value of NFT in rbt to be shared to seller for sale");
                String rbtAmount = sysInput.readLine();
                nftSellerLogger.debug("Seller sending value of NFT to Buyer to approve ");

                output.println(rbtAmount);

                String saleAmountAuth;

                try {
                    saleAmountAuth = input.readLine();
                } catch (SocketException e) {
                    nftSellerLogger.warn("Buyer Stream Null - Buyer Details");
                    APIResponse.put("did", "");
                    APIResponse.put("tid", "null");
                    APIResponse.put("status", "Failed");
                    APIResponse.put("message", "Buyer Stream Null - Buyer Detail");

                     
                    output.close();
                    input.close();
                    sk.close();
                    ss.close();
                    return APIResponse.toString();
                }

                if (saleAmountAuth != null && !saleAmountAuth.equals("200")) {
                    nftSellerLogger.debug("Buyer did not agree to RBT amount for NFT ");
                    APIResponse.put("message", "Buyer did not agree to RBT amount for NFT ");
                    APIResponse.put("did", sellerDidIpfsHash);
                    APIResponse.put("tid", "");
                    APIResponse.put("status", "Failed");
                    IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                    output.close();
                    input.close();
                     
                    sk.close();
                    ss.close();
                    return APIResponse.toString();
                }

                nftSellerLogger.debug("Buyer agreed to RBT amount for NFT");

                String balanceAuth;

                try {
                    balanceAuth = input.readLine();
                    nftSellerLogger.debug("Balamnce Auth sent from seller: "+balanceAuth);
                } catch (SocketException e) {
                    nftSellerLogger.warn("Buyer Stream Null - Buyer Details");
                    APIResponse.put("did", "");
                    APIResponse.put("tid", "null");
                    APIResponse.put("status", "Failed");
                    APIResponse.put("message", "Buyer Stream Null - Buyer Detail");

                     
                    output.close();
                    input.close();
                    sk.close();
                    ss.close();
                    return APIResponse.toString();
                }

                if (balanceAuth != null && !balanceAuth.equals("200")) {
                    nftSellerLogger.debug("Buyer does not have enough RBT balance for Purchase of NFT");
                    APIResponse.put("message", "Buyer does not have enough RBT balance for Purchase of NFT");
                    APIResponse.put("did", sellerDidIpfsHash);
                    APIResponse.put("tid", "");
                    APIResponse.put("status", "Failed");
                    IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                    output.close();
                    input.close();
                     
                    sk.close();
                    ss.close();
                    return APIResponse.toString();
                }

                nftSellerLogger.debug("Buyer has required balance for NFT purchase");

                nftSellerLogger.debug("Seller Creating sale contract");

                nftSellerLogger.info("*******************************************************");

                char[] privateKeyPass = PasswordField.getPassword(System.in, "Enter the privateKey password: ");

                nftSellerLogger.info("*******************************************************");
                JSONObject data = new JSONObject();

                data.put("sellerDID", sellerDidIpfsHash);
                data.put("nftToken", nftTokenIpfsHash);
                data.put("rbtAmount", Double.parseDouble(rbtAmount));
                data.put("sellerPvtKeyPass", String.valueOf(privateKeyPass));
                String saleContract = createNftSaleContract(data.toString());

                JSONObject responseObj = new JSONObject(saleContract);

                JSONObject saleObject = new JSONObject();
                if (responseObj.getString("status").equals("Failed")) {
                    saleObject.put("saleContractIpfsHash", "");
                    saleObject.put("auth", "420");
                    output.println(saleObject.toString());
                    nftSellerLogger.debug("NFT Sale contract creation failed");
                    APIResponse.put("message",
                            "NFT Sale contract creation Failed : " + responseObj.getString("message"));
                    APIResponse.put("did", sellerDidIpfsHash);
                    APIResponse.put("tid", "");
                    APIResponse.put("status", "Failed");
                    IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                    output.close();
                    input.close();
                     
                    sk.close();
                    ss.close();
                    return APIResponse.toString();
                }

                saleObject.put("saleContractIpfsHash", responseObj.getString("saleContractIpfsHash"));
                saleObject.put("auth", "200");

                nftSellerLogger.debug("sending sale contract details to buyer");
                output.println(saleObject.toString());

                
                nftSellerLogger.debug("sending sellers public key details to buyer");
                String sellerPublicKeyIpfsHash = getPubKeyIpfsHash();

                output.println(sellerPublicKeyIpfsHash);
            }

            nftSellerLogger.debug("Seller sending NFT token details to Buyer to verify");

            IPFSNetwork.add(NFT_TOKENS_PATH + nftTokenIpfsHash, ipfs);
            String nftTokenChainIpfsHash = IPFSNetwork.add(NFT_TOKENCHAIN_PATH + nftTokenIpfsHash + ".json", ipfs);

            JSONObject nftTokenDetails = new JSONObject();
            nftTokenDetails.put("nftToken", nftTokenIpfsHash);
            nftTokenDetails.put("nftTokenChain", nftTokenChainIpfsHash);
            nftTokenDetails.put("sellerDid", sellerDidIpfsHash);
            String nftID = calculateHash(nftTokenDetails.toString(), "SHA3-256");

            writeToFile(LOGGER_PATH + "nftID", nftID, false);
            String nftConsensusID = IPFSNetwork.addHashOnly(LOGGER_PATH + "nftID", ipfs);
            deleteFile(LOGGER_PATH + "nftID");

            nftSellerLogger.debug("Sending nft ConsensusID " + nftConsensusID + "to Buyer");
            nftTokenDetails.put("nftConsensusID", nftConsensusID);
            output.println(nftTokenDetails.toString());

            String nftTokenAuth;

            try {
                nftTokenAuth = input.readLine();
            } catch (SocketException e) {
                nftSellerLogger.warn("Buyer Stream Null - Buyer Details");
                APIResponse.put("did", "");
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "Buyer Stream Null - Buyer Detail");

                output.close();
                 
                input.close();
                sk.close();
                ss.close();
                return APIResponse.toString();
            }

            if (nftTokenAuth != null && nftTokenAuth.startsWith("4")) {
                switch (nftTokenAuth) {
                    case "419":
                        nftSellerLogger.info("NFT Token " + nftTokenIpfsHash + " is of RAC Type 1 which is depricated");
                        APIResponse.put("message", "NFT Token " + nftTokenIpfsHash + " is of RAC Type 1 which is depricated");
                    case "420":
                        nftSellerLogger.info("NFT Token Authenticity check Failed");
                        APIResponse.put("message", "NFT Token " + nftTokenIpfsHash + "Authenticity check Failed.");
                        break;
                    case "421":
                        nftSellerLogger.info("NFT Consensus ID not unique.");
                        APIResponse.put("message", "NFT Consensus ID not unique.");
                        break;
                    case "422":
                        nftSellerLogger.info("NFT Token ownership failed for " + nftTokenIpfsHash);
                        APIResponse.put("message", "TNFT Token ownership failed for " + nftTokenIpfsHash);
                        break;
                    case "423":
                        nftSellerLogger.info("NFT Token "+nftTokenIpfsHash+" has Expired");
                        APIResponse.put("message", "NFT Token "+nftTokenIpfsHash+" has Expired");

                }
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                APIResponse.put("did", sellerDidIpfsHash);
                APIResponse.put("tid", "");
                APIResponse.put("status", "Failed");
                return APIResponse.toString();
            }
            nftSellerLogger.debug("NFT token auth code " + nftTokenAuth);

            nftSellerLogger.debug("Seller requesting RBT details from Buyer");

            String tokenDetails;
            try {
                tokenDetails = input.readLine();
            } catch (SocketException e) {
                nftSellerLogger.warn("Sender Stream Null - Token Details");
                APIResponse.put("did", "");
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "Sender Stream Null - Token Details");

                 
                output.close();
                input.close();
                sk.close();
                ss.close();
                return APIResponse.toString();

            }
            JSONObject tokenObject = new JSONObject(tokenDetails);
            JSONObject TokenDetails = tokenObject.getJSONObject("tokenDetails");
            JSONArray wholeTokens = TokenDetails.getJSONArray("whole-tokens");
            JSONArray wholeTokenChains = TokenDetails.getJSONArray("whole-tokenChains");

            JSONArray partTokens = TokenDetails.getJSONArray("part-tokens");
            JSONObject partTokenChains = TokenDetails.getJSONObject("part-tokenChains");
            JSONArray partTokenChainsHash = TokenDetails.getJSONArray("hashSender");

            JSONArray previousSendersArray = tokenObject.getJSONArray("previousSender");
            JSONArray positionsArray = tokenObject.getJSONArray("positions");
            Double amount = tokenObject.getDouble("amount");
            JSONObject amountLedger = tokenObject.getJSONObject("amountLedger");
            nftSellerLogger.debug("Amount Ledger: " + amountLedger);
            int intPart = wholeTokens.length();
            Double decimalPart = formatAmount(amount - intPart);
            JSONArray doubleSpentToken = new JSONArray();
            /*
             * boolean tokenOwners = true;
             * ArrayList ownersArray = new ArrayList();
             * ArrayList previousSender = new ArrayList();
             * JSONArray ownersReceived = new JSONArray();
             * for (int i = 0; i < wholeTokens.length(); ++i) {
             * try {
             * nftSellerLogger.debug("Checking owners for " + wholeTokens.getString(i) +
             * " Please wait...");
             * ownersArray = IPFSNetwork.dhtOwnerCheck(wholeTokens.getString(i));
             * 
             * if (ownersArray.size() > 2) {
             * 
             * for (int j = 0; j < previousSendersArray.length(); j++) {
             * if (previousSendersArray.getJSONObject(j).getString("token")
             * .equals(wholeTokens.getString(i)))
             * ownersReceived =
             * previousSendersArray.getJSONObject(j).getJSONArray("sender");
             * }
             * 
             * for (int j = 0; j < ownersReceived.length(); j++) {
             * previousSender.add(ownersReceived.getString(j));
             * }
             * nftSellerLogger.debug("Previous Owners: " + previousSender);
             * 
             * for (int j = 0; j < ownersArray.size(); j++) {
             * if (!previousSender.contains(ownersArray.get(j).toString()))
             * tokenOwners = false;
             * }
             * }
             * } catch (IOException e) {
             * 
             * nftSellerLogger.debug("Ipfs dht find did not execute");
             * }
             * }
             * if (!tokenOwners) {
             * JSONArray owners = new JSONArray();
             * for (int i = 0; i < ownersArray.size(); i++)
             * owners.put(ownersArray.get(i).toString());
             * nftSellerLogger.debug("Multiple Owners for " + doubleSpentToken);
             * nftSellerLogger.debug("Owners: " + owners);
             * output.println("420");
             * output.println(doubleSpentToken.toString());
             * output.println(owners.toString());
             * APIResponse.put("did", sellerDidIpfsHash);
             * APIResponse.put("tid", "null");
             * APIResponse.put("status", "Failed");
             * APIResponse.put("message", "Multiple Owners for " + doubleSpentToken + " " +
             * owners);
             * IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
             * output.close();
             * input.close();
             *  
             * sk.close();
             * ss.close();
             * return APIResponse.toString();
             * }
             */

            String senderToken = TokenDetails.toString();
            String consensusID = calculateHash(senderToken, "SHA3-256");
            writeToFile(LOGGER_PATH + "consensusID", consensusID, false);
            String consensusIDIPFSHash = IPFSNetwork.addHashOnly(LOGGER_PATH + "consensusID", ipfs);
            deleteFile(LOGGER_PATH + "consensusID");

            if (!IPFSNetwork.dhtEmpty(consensusIDIPFSHash, ipfs)) {
                nftSellerLogger.debug("consensus ID not unique" + consensusIDIPFSHash);
                output.println("421");
                APIResponse.put("did", sellerDidIpfsHash);
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "Consensus ID not unique");
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();
            }

            // Check IPFS get for all Tokens
            int ipfsGetFlag = 0;
            ArrayList<String> wholeTokenContent = new ArrayList<>();
            ArrayList<String> wholeTokenChainContent = new ArrayList<>();
            for (int i = 0; i < intPart; i++) {
                String TokenChainContent = IPFSNetwork.get(wholeTokenChains.getString(i), ipfs);
                wholeTokenChainContent.add(TokenChainContent);
                String TokenContent = IPFSNetwork.get(wholeTokens.getString(i), ipfs);
                wholeTokenContent.add(TokenContent);
                ipfsGetFlag++;
            }
            IPFSNetwork.repo(ipfs);

            if (!(ipfsGetFlag == intPart)) {
                output.println("422");
                APIResponse.put("did", sellerDidIpfsHash);
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "Tokens not verified");
                nftSellerLogger.info("Tokens not verified");
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();
            }

            JSONArray partTokenChainContent = new JSONArray();
            JSONArray partTokenContent = new JSONArray();

            for (int i = 0; i < partTokenChains.length(); i++) {

                partTokenChainContent.put(partTokenChains.getJSONArray(partTokens.getString(i)));
                String TokenContent = IPFSNetwork.get(partTokens.getString(i), ipfs);
                partTokenContent.put(TokenContent);
            }

            boolean chainFlag = true;
            for (int i = 0; i < partTokenChainContent.length(); i++) {
                JSONArray tokenChainContent = partTokenChainContent.getJSONArray(i);
                for (int j = 0; j < tokenChainContent.length(); j++) {
                    String previousHash = tokenChainContent.getJSONObject(j).getString("previousHash");
                    String nextHash = tokenChainContent.getJSONObject(j).getString("nextHash");
                    String rePreviousHash, reNextHash;
                    if (tokenChainContent.length() > 1) {
                        if (j == 0) {
                            rePreviousHash = "";
                            String rePrev = calculateHash(new JSONObject().toString(), "SHA3-256");
                            reNextHash = calculateHash(tokenChainContent.getJSONObject(j + 1).getString("tid"),
                                    "SHA3-256");

                            if (!((rePreviousHash.equals(previousHash) || rePrev.equals(previousHash))
                                    && reNextHash.equals(nextHash))) {
                                chainFlag = false;
                            }

                        } else if (j == tokenChainContent.length() - 1) {
                            rePreviousHash = calculateHash(tokenChainContent.getJSONObject(j - 1).getString("tid"),
                                    "SHA3-256");
                            reNextHash = "";

                            if (!(rePreviousHash.equals(previousHash) && reNextHash.equals(nextHash))) {
                                chainFlag = false;
                            }

                        } else {
                            rePreviousHash = calculateHash(tokenChainContent.getJSONObject(j - 1).getString("tid"),
                                    "SHA3-256");
                            reNextHash = calculateHash(tokenChainContent.getJSONObject(j + 1).getString("tid"),
                                    "SHA3-256");

                            if (!(rePreviousHash.equals(previousHash) && reNextHash.equals(nextHash))) {
                                chainFlag = false;
                            }
                        }
                    }
                }
            }
            if (!chainFlag) {

                String errorMessage = "Broken Cheque Chain";
                output.println("423");
                APIResponse.put("did", sellerDidIpfsHash);
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", errorMessage);
                nftSellerLogger.debug(errorMessage);
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();
            }

            // owner check logic new
            boolean ownerCheck = true;

            JSONArray allTokens = new JSONArray();
            for (int i = 0; i < wholeTokens.length(); i++)
                allTokens.put(wholeTokens.getString(i));
            for (int i = 0; i < partTokens.length(); i++)
                allTokens.put(partTokens.getString(i));

            JSONArray allTokensChains = new JSONArray();
            for (int i = 0; i < wholeTokenChainContent.size(); i++)
                allTokensChains.put(wholeTokenChainContent.get(i));
            for (int i = 0; i < partTokenChainContent.length(); i++)
                allTokensChains.put(partTokenChainContent.get(i));

            nftSellerLogger.debug("$$$$$$$$$$$$$$$$$$$$");    
            nftSellerLogger.debug("Content of WholeToken JSONArray " + wholeTokens.toString());
            nftSellerLogger.debug("Content of Alltoken JSONArray " + allTokens.toString());    
            nftSellerLogger.debug("$$$$$$$$$$$$$$$$$$$$");

            JSONArray invalidTokens = new JSONArray();
            
            for (int count = 0; count < wholeTokenChains.length(); count++) {//changed upperlimit from alltokenchain.length to wholetokenchain.length

                String tokens = null;
                JSONArray tokenChain = new JSONArray(wholeTokenChains.get(count).toString());//changed from alltokenchain to wholetokenchain
                String TokenContent = IPFSNetwork.get(wholeTokens.getString(count), ipfs);
                String tokenLevel = TokenContent.substring(0, TokenContent.length() - 64);
                String tokenNumberHash = TokenContent.substring(TokenContent.length() - 64);
                // String tokenLevel = TokenContent.substring(0, 3);
                // String tokenNumberHash = TokenContent.substring(3,
                // TokenContent.indexOf("\n"));

                int tokenLevelInt = Integer.parseInt(tokenLevel);
                int tokenLimitForLevel = tokenLimit[tokenLevelInt];
                int tokenLevelValue = (int) Math.pow(2, tokenLevelInt + 2);
                int minumumStakeHeight = tokenLevelValue * 4;
                int tokenNumber = 1204401;

                // check TokenHashTable exists
                File tokenHashTable = new File(
                        WALLET_DATA_PATH.concat("TokenHashTable").concat(".json"));
                if (!tokenHashTable.exists()) {
                    tokenHashTable.createNewFile();
                    JSONObject tokenHashTableJSON = new JSONObject();
                    for (int i = 1; i <= 5000000; i++) {
                        tokenHashTableJSON.put(calculateHash(String.valueOf(i), "SHA-256"), i);
                    }
                    writeToFile(tokenHashTable.toString(), tokenHashTableJSON.toString(), false);
                }
                String tokenHashTableData = readFile(tokenHashTable.toString());
                JSONObject tokenHashTableJSON = new JSONObject(tokenHashTableData);
                if (tokenHashTableJSON.has(tokenNumberHash)) {
                    tokenNumber = tokenHashTableJSON.getInt(tokenNumberHash);
                    nftSellerLogger.debug("Token Number: " + tokenNumber);
                    if (tokenNumber > tokenLimitForLevel) {
                        String errorMessage = "Token Number is greater than Token Limit for the Level";
                        output.println("426");
                        APIResponse.put("did", buyerDidIpfsHash);
                        APIResponse.put("tid", "null");
                        APIResponse.put("status", "Failed");
                        APIResponse.put("message", errorMessage);
                        nftSellerLogger.debug(errorMessage);
                        IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                        output.close();
                        input.close();
                        sk.close();
                        ss.close();
                        return APIResponse.toString();
                    }
                } else {
                    nftSellerLogger.debug("Invalid Content Found in Token : " + tokenNumberHash);
                    String errorMessage = "Invalid Content Found in Token";
                    output.println("426");
                    APIResponse.put("did", buyerDidIpfsHash);
                    APIResponse.put("tid", "null");
                    APIResponse.put("status", "Failed");
                    APIResponse.put("message", errorMessage);
                    nftSellerLogger.debug(errorMessage);
                    IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                    output.close();
                    input.close();
                    sk.close();
                    ss.close();
                    return APIResponse.toString();
                }

                if ((tokenNumber >= 1204400) && (tokenLevelInt >= 4)) {

                    JSONObject lastObject = tokenChain.getJSONObject(tokenChain.length() - 1);
                    nftSellerLogger.debug("Last Object = " + lastObject);

                    if (lastObject.has("owner") && !lastObject.has(STAKED_TOKEN)) {

                        nftSellerLogger.debug("Checking ownership");
                        String owner = lastObject.getString("owner");
                        tokens = allTokens.getString(count);
                        String hashString = tokens.concat(buyerDidIpfsHash);
                        String hashForPositions = calculateHash(hashString, "SHA3-256");
                        String ownerIdentity = hashForPositions.concat(positionsArray.getString(count));
                        String ownerRecalculated = calculateHash(ownerIdentity, "SHA3-256");

                        nftSellerLogger.debug("Ownership Here Sender Calculation");
                        nftSellerLogger.debug("tokens: " + tokens);
                        nftSellerLogger.debug("hashString: " + hashString);
                        nftSellerLogger.debug("hashForPositions: " + hashForPositions);
                        nftSellerLogger.debug("p1: " + positionsArray.getString(count));
                        nftSellerLogger.debug("ownerIdentity: " + ownerIdentity);
                        nftSellerLogger.debug("ownerIdentityHash: " + ownerRecalculated);

                        if (!owner.equals(ownerRecalculated)) {
                            ownerCheck = false;
                            invalidTokens.put(tokens);
                        }

                        // ! staking checks (1..4) starts here

                        // ! staking checks (1): Check incoming token level

                        if (ownerCheck && (tokenChain.length() < minumumStakeHeight)) {
                            // && (tokenNumber > 1204400)

                            // ! staking checks (3): Verify the signatures earned during the mining of the
                            // ! incoming mint token
                            JSONObject genesiObject = tokenChain.getJSONObject(0);
                            JSONArray stakeDataArray = genesiObject.getJSONArray(MINE_ID);

                            int randomNumber = new Random().nextInt(15);
                            JSONObject genesisSignaturesContent = genesiObject
                                    .getJSONObject(QUORUM_SIGN_CONTENT);
                            Iterator randomKey = genesisSignaturesContent.keys();
                            for (int i = 0; i < randomNumber; i++) {
                                randomKey.next();
                            }
                            /**
                             * String randomKeyString = randomKey.next().toString();
                             * JSONObject verificationPick = new JSONObject();
                             * verificationPick.put("did", randomKeyString);
                             * verificationPick.put("hash", genesiObject.getString("tid"));
                             * verificationPick.put("signature",
                             * genesisSignaturesContent.getString(randomKeyString));
                             * 
                             * if (verificationPick.getString("hash").equals(genesiObject.getString("tid")))
                             * {
                             * 
                             * if (Authenticate.verifySignature(verificationPick.toString())) {
                             * nftSellerLogger.debug("Staking check (3) successful");
                             * } else {
                             * nftSellerLogger.debug(
                             * "Staking check (3) failed: Could not verify genesis credit signature");
                             * ownerCheck = false;
                             * invalidTokens.put(tokens);
                             * }
                             * } else {
                             * nftSellerLogger.debug(
                             * "Staking check (3) failed: Genesis TID is not equal to the hash of the
                             * genesis signature");
                             * ownerCheck = false;
                             * invalidTokens.put(tokens);
                             * }
                             */
                            // else {
                            // nftSellerLogger.debug("Staking check (3) failed: Genesis Signature not
                            // found");
                            // ownerCheck = false;
                            // invalidTokens.put(tokens);
                            // }

                            // ! staking checks (2): For incoming new mint token, verify the staked token

                            if (stakeDataArray.length() == 3) {

                                JSONObject oneOfThreeStake = stakeDataArray.getJSONObject(0);
                                JSONObject twoOfThreeStake = stakeDataArray.getJSONObject(1);
                                JSONObject threeOfThreeStake = stakeDataArray.getJSONObject(2);

                                String[] stakedTokenTC = new String[3];
                                String[] stakedTokenSignTC = new String[3];
                                String[] stakerDIDTC = new String[3];
                                String[] mineIDTC = new String[3];
                                String[] mineIDSignTC = new String[3];

                                stakedTokenTC[0] = oneOfThreeStake.getString(STAKED_TOKEN);
                                stakedTokenSignTC[0] = oneOfThreeStake.getString(STAKED_TOKEN_SIGN);
                                stakerDIDTC[0] = oneOfThreeStake.getString(STAKED_QUORUM_DID);
                                mineIDTC[0] = oneOfThreeStake.getString(MINE_ID);
                                mineIDSignTC[0] = oneOfThreeStake.getString(MINE_ID_SIGN);

                                stakedTokenTC[1] = twoOfThreeStake.getString(STAKED_TOKEN);
                                stakedTokenSignTC[1] = twoOfThreeStake.getString(STAKED_TOKEN_SIGN);
                                stakerDIDTC[1] = twoOfThreeStake.getString(STAKED_QUORUM_DID);
                                mineIDTC[1] = twoOfThreeStake.getString(MINE_ID);
                                mineIDSignTC[1] = twoOfThreeStake.getString(MINE_ID_SIGN);

                                stakedTokenTC[2] = threeOfThreeStake.getString(STAKED_TOKEN);
                                stakedTokenSignTC[2] = threeOfThreeStake.getString(STAKED_TOKEN_SIGN);
                                stakerDIDTC[2] = threeOfThreeStake.getString(STAKED_QUORUM_DID);
                                mineIDTC[2] = threeOfThreeStake.getString(MINE_ID);
                                mineIDSignTC[2] = threeOfThreeStake.getString(MINE_ID_SIGN);

                                for (int stakeCount = 0; stakeCount < mineIDTC.length; stakeCount++) {

                                    String mineIDContent = IPFSNetwork.get(mineIDTC[stakeCount], ipfs);
                                    JSONObject mineIDContentJSON = new JSONObject(mineIDContent);
                                    nftSellerLogger.debug(mineIDContentJSON.toString());

                                    JSONObject stakeData = mineIDContentJSON.getJSONObject(STAKE_DATA);

                                    String stakerDIDMineData = stakeData.getString(STAKED_QUORUM_DID);
                                    String stakedTokenMineData = stakeData.getString(STAKED_TOKEN);
                                    String stakedTokenSignMineData = stakeData.getString(STAKED_TOKEN_SIGN);

                                    nftSellerLogger.debug(stakerDIDTC[stakeCount]);
                                    nftSellerLogger.debug(stakedTokenTC[stakeCount]);
                                    nftSellerLogger.debug(stakedTokenSignTC[stakeCount]);

                                    nftSellerLogger.debug(stakerDIDMineData);
                                    nftSellerLogger.debug(stakedTokenMineData);
                                    nftSellerLogger.debug(stakedTokenSignMineData);

                                    if (stakerDIDTC[stakeCount].equals(stakerDIDMineData)
                                            && stakedTokenTC[stakeCount].equals(stakedTokenMineData)
                                            && stakedTokenSignTC[stakeCount].equals(stakedTokenSignMineData)) {

                                        JSONObject detailsToVerify = new JSONObject();
                                        detailsToVerify.put("did", stakerDIDTC[stakeCount]);
                                        detailsToVerify.put("hash", mineIDTC[stakeCount]);
                                        detailsToVerify.put("signature", mineIDSignTC[stakeCount]);
                                        if (Authenticate.verifySignature(detailsToVerify.toString())) {

                                            boolean minedTokenStatus = true;
                                            ArrayList<String> ownersArray = IPFSNetwork
                                                    .dhtOwnerCheck(stakedTokenTC[stakeCount]);
                                            for (int i = 0; i < ownersArray.size(); i++) {
                                                if (ownersArray.get(i).equals(stakerDIDTC[stakeCount])) {
                                                    minedTokenStatus = false;
                                                }
                                            }
                                            if (!minedTokenStatus) {
                                                nftSellerLogger.debug(
                                                        "Staked token is not found with staker DID: "
                                                                + stakerDIDTC[stakeCount]);
                                                ownerCheck = false;
                                                invalidTokens.put(tokens);
                                            }

                                        } else {
                                            nftSellerLogger.debug(
                                                    "Staking check (2) failed - unable to verify mine ID signature by staker: "
                                                            + stakerDIDTC[stakeCount]);
                                            ownerCheck = false;
                                            invalidTokens.put(tokens);
                                        }

                                        nftSellerLogger
                                                .debug("MineID Verification Successful with Staking node: "
                                                        + stakerDIDTC[stakeCount]);
                                    } else {
                                        nftSellerLogger.debug("Staking check (2) failed");
                                        ownerCheck = false;
                                        invalidTokens.put(tokens);
                                    }

                                    nftSellerLogger.debug("Staking check (2) successful");
                                    // } else {
                                    // nftSellerLogger.debug(
                                    // "Staking check (2) failed: Could not verify mine ID signature");
                                    // ownerCheck = false;
                                    // invalidTokens.put(tokens);
                                    // }
                                }

                            } else {
                                ownerCheck = false;
                                nftSellerLogger.debug("Staked Token is not available!");

                            }

                        }
                    }
                    if (lastObject.has(STAKED_TOKEN)) {

                        Boolean minedTokenStatus = true;

                        String mineID = lastObject.getString(MINE_ID);

                        String mineIDContent = IPFSNetwork.get(mineID, ipfs);
                        JSONObject mineIDContentJSON = new JSONObject(mineIDContent);

                        JSONObject stakeData = mineIDContentJSON.getJSONObject(STAKE_DATA);

                        ArrayList<String> ownersArray = IPFSNetwork.dhtOwnerCheck(stakeData.getString(STAKED_TOKEN));
                        for (int i = 0; i < ownersArray.size(); i++) {
                            if (!VerifyStakedToken.Contact(ownersArray.get(i), SEND_PORT + 16,
                                    stakeData.getString(
                                            STAKED_TOKEN),
                                    mineIDContentJSON.getString("tokenContent"))) {
                                minedTokenStatus = false;
                            }
                        }
                        if (!minedTokenStatus) {
                            nftSellerLogger
                                    .debug("Staking check failed: Found staked token but token height < 46");
                            ownerCheck = false;
                            invalidTokens.put(tokens);
                        }

                        nftSellerLogger.debug(
                                "Staking check failed: Found staked token but unable to transfer while mined token height is not satisfied for the network");
                        ownerCheck = false;
                        invalidTokens.put(tokens);

                        // JSONObject tokenToVerify = new JSONObject();
                        // if (mineIDContentJSON.has(MiningConstants.STAKE_DATA)) {

                        // JSONObject stakeData =
                        // mineIDContentJSON.getJSONObject(MiningConstants.STAKE_DATA);
                        // String stakerDID = stakeData.getString(STAKED_QUORUM_DID);
                        // String stakedToken = stakeData.getString(STAKED_TOKEN);
                        // String stakedTokenSign = stakeData.getString(STAKED_TOKEN_SIGN);

                        // tokenToVerify.put("did", senderDidIpfsHash);
                        // tokenToVerify.put("hash", stakedToken);
                        // tokenToVerify.put("signature", stakedTokenSign);

                        // if (Authenticate.verifySignature(tokenToVerify.toString())) {

                        // ArrayList<String> ownersArray = IPFSNetwork.dhtOwnerCheck(stakedToken);
                        // for (int i = 0; i < ownersArray.size(); i++) {
                        // if (!VerifyStakedToken.Contact(ownersArray.get(i), SEND_PORT + 16,
                        // mineIDContentJSON.getString("tokenContent"))) {
                        // minedTokenStatus = false;
                        // }
                        // }
                        // if (!minedTokenStatus) {
                        // nftSellerLogger
                        // .debug("Staking check failed: Found staked token but token height < 46");
                        // ownerCheck = false;
                        // invalidTokens.put(tokens);
                        // }

                        // nftSellerLogger.debug(
                        // "Staking check failed: Found staked token but unable to transfer while mined
                        // token height is not satisfied for the network");
                        // ownerCheck = false;
                        // invalidTokens.put(tokens);

                        // } else {
                        // nftSellerLogger.debug(
                        // "Staking check failed: Found staked token but unable to verify staked token
                        // height");
                        // ownerCheck = false;
                        // invalidTokens.put(tokens);
                        // }
                        // }

                    }
                }
            }

            if (!ownerCheck) {
                nftSellerLogger.debug("Ownership Check Failed");
                String errorMessage = "Ownership Check Failed";
                output.println("424");
                output.println(invalidTokens.toString());
                APIResponse.put("did", sellerDidIpfsHash);
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", errorMessage);
                nftSellerLogger.debug(errorMessage);
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();
            } else
                nftSellerLogger.debug("Ownership Check Passed");

            boolean partsAvailable = true;
            for (int i = 0; i < partTokenChainContent.length(); i++) {
                Double senderCount = 0.000D, receiverCount = 0.000D;
                JSONArray tokenChainContent = partTokenChainContent.getJSONArray(i);
                for (int k = 0; k < tokenChainContent.length(); k++) {
                    if (tokenChainContent.getJSONObject(k).has("role")) {
                        if (tokenChainContent.getJSONObject(k).getString("role").equals("Sender")
                                && tokenChainContent.getJSONObject(k).getString("sender").equals(buyerDidIpfsHash)) {
                            senderCount += tokenChainContent.getJSONObject(k).getDouble("amount");
                        } else if (tokenChainContent.getJSONObject(k).getString("role").equals("Receiver")
                                && tokenChainContent.getJSONObject(k).getString("receiver").equals(buyerDidIpfsHash)) {
                            receiverCount += tokenChainContent.getJSONObject(k).getDouble("amount");
                        }
                    }
                }
                FunctionsLogger.debug("Sender Parts: " + formatAmount(senderCount));
                FunctionsLogger.debug("Receiver Parts: " + formatAmount(receiverCount));
                Double availableParts = receiverCount - senderCount;

                availableParts = formatAmount(availableParts);
                availableParts += amountLedger.getDouble(partTokens.getString(i));
                availableParts = formatAmount(availableParts);

                if (availableParts > 1.000D) {
                    nftSellerLogger.debug("Token wholly spent: " + partTokens.getString(i));
                    nftSellerLogger.debug("Parts: " + availableParts);
                    partsAvailable = false;
                }
            }

            if (!partsAvailable) {
                String errorMessage = "Token wholly spent already";
                output.println("425");
                APIResponse.put("did", sellerDidIpfsHash);
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", errorMessage);
                nftSellerLogger.debug(errorMessage);
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();
            } else {
                output.println("200");
            }

            String saleContractAuth;
            try {
                saleContractAuth = input.readLine();
            } catch (SocketException e) {
                nftSellerLogger.warn("Buyer Stream Null - Buyer Details");
                APIResponse.put("did", sellerDidIpfsHash);
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "Buyer Stream Null - Buyer Detail");

                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();
            }

            if (saleContractAuth != null && !saleContractAuth.equals("200")) {
                nftSellerLogger.info("Buyer was not able to verify sale contract");
                APIResponse.put("message", "Buyer " + buyerDidIpfsHash + " was not able to verify sale contract");
                APIResponse.put("did", sellerDidIpfsHash);
                APIResponse.put("tid", "");
                APIResponse.put("status", "Failed");
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();
            }

            nftSellerLogger.info("Buyer verified NFT sale contract");

            String consensusDetails;

            try {
                consensusDetails = input.readLine();
            } catch (SocketException e) {
                nftSellerLogger.warn("Buyer Stream Null - Buyer Details");
                APIResponse.put("did", sellerDidIpfsHash);
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "Buyer Stream Null - Buyer Detail");

                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();
            }

            nftSellerLogger.debug("Recived quorum consesnsus data");

            JSONObject SenderDetails = new JSONObject(consensusDetails);
            String senderSignature = SenderDetails.getString("sign");
            String tid = SenderDetails.getString("tid");
            String comment = SenderDetails.getString("comment");
            String Status = SenderDetails.getString("status");
            String QuorumDetails = SenderDetails.getString("quorumsign");
            String nftQuorumDetails= SenderDetails.getString("nftQuorumSign");

            JSONObject nftQuorumSig = new JSONObject(nftQuorumDetails);

            BufferedImage senderWidImage = ImageIO.read(new File(DATA_PATH + buyerDidIpfsHash + "/PublicShare.png"));
            String SenWalletBin = PropImage.img2bin(senderWidImage);

            nftSellerLogger.debug("Verifying Quorum ...  ");
            nftSellerLogger.debug("Please wait, this might take a few seconds");

            if (!Status.equals("Consensus Failed")) {
                boolean yesQuorum = false;
                nftSellerLogger.debug("Consensus Status got from nftBuyer " + Status);
                if (Status.equals("Consensus Reached")) {
                    quorumSignatures = new JSONObject(QuorumDetails);

                    String selectQuorumHash = calculateHash(senderToken, "SHA3-256");
                    String verifyQuorumHash = calculateHash(selectQuorumHash.concat(sellerDidIpfsHash), "SHA3-256");

                    nftSellerLogger.debug("details that Seller use to put RBT Signature");
                    nftSellerLogger.debug("1: " + selectQuorumHash);
                    nftSellerLogger.debug("2: " + sellerDidIpfsHash);
                    nftSellerLogger.debug("Quorum hash: " + verifyQuorumHash);

                    Iterator<String> keys = quorumSignatures.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        quorumDID.add(key);
                    }

                    for (String quorumDidIpfsHash : quorumDID) {
                        syncDataTable(quorumDidIpfsHash, null);
                        String quorumWidIpfsHash = getValues(DATA_PATH + "DataTable.json", "walletHash", "didHash",
                                quorumDidIpfsHash);

                        nodeData(quorumDidIpfsHash, quorumWidIpfsHash, ipfs);
                    }

                    for (int i = 0; i < quorumSignatures.length(); i++) {

                        JSONObject detailsForVerify = new JSONObject();
                        detailsForVerify.put("did", quorumDID.get(i));
                        detailsForVerify.put("hash", verifyQuorumHash);
                        detailsForVerify.put("signature", quorumSignatures.getString(quorumDID.get(i)));
                        boolean val = Authenticate.verifySignature(detailsForVerify.toString());
                        if (val)
                            quorumSignVerifyCount++;
                    }
                    nftSellerLogger.debug("Verified Quorum Count " + quorumSignVerifyCount);

                    yesQuorum = quorumSignVerifyCount >= quorumSignatures.length();
                }
                JSONArray wholeTokenChainHash = new JSONArray();
                for (int i = 0; i < intPart; i++)
                    wholeTokenChainHash.put(wholeTokenChains.getString(i));

                String hash = calculateHash(
                        wholeTokens.toString() + wholeTokenChainHash.toString() + partTokens.toString()
                                + partTokenChainsHash.toString() + buyerDidIpfsHash + sellerDidIpfsHash + comment,
                        "SHA3-256");
                nftSellerLogger.debug("Hash to verify Sender: " + hash);
                JSONObject detailsForVerify = new JSONObject();
                detailsForVerify.put("did", buyerDidIpfsHash);
                detailsForVerify.put("hash", hash);
                detailsForVerify.put("signature", senderSignature);

                boolean yesSender = Authenticate.verifySignature(detailsForVerify.toString());
                nftSellerLogger.debug("Quorum Auth : " + yesQuorum + " Sender Auth : " + yesSender);
                if (!(yesSender && yesQuorum)) {
                    output.println("420");
                    APIResponse.put("did", sellerDidIpfsHash);
                    APIResponse.put("tid", tid);
                    APIResponse.put("status", "Failed");
                    APIResponse.put("message", "Sender / Quorum not verified");
                    nftSellerLogger.info("Sender / Quorum not verified");
                    IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                    output.close();
                    input.close();
                     
                    sk.close();
                    ss.close();
                    return APIResponse.toString();
                }

                // IPFSNetwork.repo(ipfs);
                nftSellerLogger.debug("Sender and Quorum Verified");
                output.println("200");

                IPFSNetwork.unpin(nftTokenIpfsHash, ipfs);
                // IPFSNetwork.repo(ipfs);

                output.println("NFT-UnPinned");

                nftSellerLogger.debug("NFT-UnPinned");
                nftSellerLogger.debug("Waiting for NFT Buyer to unpin RBT tokens");
                String rbtUnpin;

                try {
                    rbtUnpin = input.readLine();
                } catch (SocketException e) {
                    nftSellerLogger.warn("Sender Stream Null - Token Details");
                    APIResponse.put("did", "");
                    APIResponse.put("tid", "null");
                    APIResponse.put("status", "Failed");
                    APIResponse.put("message", "Sender Stream Null - Token Details");

                    output.close();
                    input.close();
                     
                    sk.close();
                    ss.close();
                    return APIResponse.toString();
                }
                nftSellerLogger.debug("Buyer Response " + rbtUnpin);
                if (rbtUnpin != null && !rbtUnpin.equals("RBT-UnPinned")) {
                    IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                    nftSellerLogger.info("Seller unpin NFT Failed");
                    output.close();
                    input.close();
                     
                    ss.close();
                    sk.close();
                    // senderMutex = false;
                    // updateQuorum(quorumArray, null, false, type);
                    APIResponse.put("did", sellerDidIpfsHash);
                    APIResponse.put("tid", tid);
                    APIResponse.put("status", "Failed");
                    APIResponse.put("message", "Buyer unpin RBT Failed");
                    return APIResponse.toString();
                }
                int count = 0;
                for (int i = 0; i < intPart; i++) {
                    FileWriter fileWriter;
                    fileWriter = new FileWriter(TOKENS_PATH + wholeTokens.getString(i));
                    fileWriter.write(wholeTokenContent.get(i));
                    fileWriter.close();
                    IPFSNetwork.add(TOKENS_PATH + wholeTokens.getString(i), ipfs);
                    IPFSNetwork.pin(wholeTokens.getString(i), ipfs);
                    count++;

                }

                for (int i = 0; i < partTokens.length(); i++) {
                    File tokenFile = new File(PART_TOKEN_PATH + partTokens.getString(i));
                    if (!tokenFile.exists())
                        tokenFile.createNewFile();
                    FileWriter fileWriter;
                    fileWriter = new FileWriter(PART_TOKEN_PATH + partTokens.getString(i));
                    fileWriter.write(partTokenContent.getString(i));
                    fileWriter.close();
                    String tokenHash = IPFSNetwork.add(PART_TOKEN_PATH + partTokens.getString(i), ipfs);
                    IPFSNetwork.pin(tokenHash, ipfs);

                }

                if (count == intPart) {
                    nftSellerLogger.debug("Pinned All Tokens");
                    output.println("Successfully Pinned");

                    String essentialShare;
                    try {
                        essentialShare = input.readLine();
                    } catch (SocketException e) {
                        nftSellerLogger.warn("Sender Stream Null - EShare Details");
                        APIResponse.put("did", "");
                        APIResponse.put("tid", "null");
                        APIResponse.put("status", "Failed");
                        APIResponse.put("message", "Sender Stream Null - EShare Details");

                        output.close();
                        input.close();
                         
                        sk.close();
                        ss.close();
                        return APIResponse.toString();

                    }
                    long endTime = System.currentTimeMillis();

                    for (int i = 0; i < intPart; i++) {

                        String tokens = wholeTokens.getString(i);
                        String hashString = tokens.concat(sellerDidIpfsHash);
                        String hashForPositions = calculateHash(hashString, "SHA3-256");

                        BufferedImage pvt = ImageIO
                                .read(new File(DATA_PATH.concat(sellerDidIpfsHash).concat("/PrivateShare.png")));
                        String firstPrivate = PropImage.img2bin(pvt);
                        int[] privateIntegerArray1 = strToIntArray(firstPrivate);
                        String privateBinary = Functions.intArrayToStr(privateIntegerArray1);
                        String positions = "";
                        for (int j = 0; j < privateIntegerArray1.length; j += 49152) {
                            positions += privateBinary.charAt(j);
                        }
                        String ownerIdentity = hashForPositions.concat(positions);
                        String ownerIdentityHash = calculateHash(ownerIdentity, "SHA3-256");

                        nftSellerLogger.debug("Ownership Here");
                        nftSellerLogger.debug("tokens: " + wholeTokens.getString(i));
                        nftSellerLogger.debug("hashString: " + hashString);
                        nftSellerLogger.debug("hashForPositions: " + hashForPositions);
                        nftSellerLogger.debug("p1: " + positions);
                        nftSellerLogger.debug("ownerIdentity: " + ownerIdentity);
                        nftSellerLogger.debug("ownerIdentityHash: " + ownerIdentityHash);

                        ArrayList<String> groupTokens = new ArrayList<>();
                        for (int k = 0; k < intPart; k++) {
                            if (!wholeTokens.getString(i).equals(wholeTokens.getString(k)))
                                groupTokens.add(wholeTokens.getString(k));
                        }

                        JSONArray arrToken = new JSONArray();
                        JSONObject objectToken = new JSONObject();
                        objectToken.put("tokenHash", wholeTokens.getString(i));
                        arrToken.put(objectToken);
                        JSONArray arr1 = new JSONArray(wholeTokenChainContent.get(i));
                        JSONObject obj2 = new JSONObject();
                        obj2.put("senderSign", senderSignature);
                        obj2.put("sender", buyerDidIpfsHash);
                        obj2.put("group", groupTokens);
                        obj2.put("comment", comment);
                        obj2.put("tid", tid);
                        obj2.put("owner", ownerIdentityHash);
                        arr1.put(obj2);
                        writeToFile(TOKENCHAIN_PATH + wholeTokens.getString(i) + ".json", arr1.toString(), false);
                    }

                    for (int i = 0; i < partTokens.length(); i++) {
                        JSONObject chequeObject = new JSONObject();
                        chequeObject.put("sender", buyerDidIpfsHash);
                        chequeObject.put("receiver", sellerDidIpfsHash);
                        chequeObject.put("parent-token", partTokens.getString(i));
                        chequeObject.put("parent-chain", partTokenChains.getJSONArray(partTokens.getString(i)));
                        Double partAmount = formatAmount(amountLedger.getDouble(partTokens.getString(i)));
                        chequeObject.put("amount", partAmount);
                        chequeObject.put("tid", tid);

                        writeToFile(LOGGER_PATH.concat(partTokens.getString(i)), chequeObject.toString(), false);
                        String chequeHash = IPFSNetwork.add(LOGGER_PATH.concat(partTokens.getString(i)), ipfs);
                        deleteFile(LOGGER_PATH.concat(partTokens.getString(i)));

                        String tokens = partTokens.getString(i);
                        String hashString = tokens.concat(sellerDidIpfsHash);
                        String hashForPositions = calculateHash(hashString, "SHA3-256");
                        BufferedImage pvt = ImageIO
                                .read(new File(DATA_PATH.concat(sellerDidIpfsHash).concat("/PrivateShare.png")));
                        String firstPrivate = PropImage.img2bin(pvt);
                        int[] privateIntegerArray1 = strToIntArray(firstPrivate);
                        String privateBinary = Functions.intArrayToStr(privateIntegerArray1);
                        String positions = "";
                        for (int j = 0; j < privateIntegerArray1.length; j += 49152) {
                            positions += privateBinary.charAt(j);
                        }

                        String ownerIdentity = hashForPositions.concat(positions);
                        String ownerIdentityHash = calculateHash(ownerIdentity, "SHA3-256");

                        nftSellerLogger.debug("Ownership Here");
                        nftSellerLogger.debug("tokens: " + partTokens.getString(i));
                        nftSellerLogger.debug("hashString: " + hashString);
                        nftSellerLogger.debug("hashForPositions: " + hashForPositions);
                        nftSellerLogger.debug("p1: " + positions);
                        nftSellerLogger.debug("ownerIdentity: " + ownerIdentity);
                        nftSellerLogger.debug("ownerIdentityHash: " + ownerIdentityHash);

                        JSONObject newPartObject = new JSONObject();
                        newPartObject.put("senderSign", senderSignature);
                        newPartObject.put("sender", buyerDidIpfsHash);
                        newPartObject.put("receiver", sellerDidIpfsHash);
                        newPartObject.put("comment", comment);
                        newPartObject.put("tid", tid);
                        newPartObject.put("nextHash", "");
                        newPartObject.put("owner", ownerIdentityHash);
                        if (partTokenChainContent.getJSONArray(i).length() == 0)
                            newPartObject.put("previousHash", "");
                        else
                            newPartObject.put("previousHash",
                                    calculateHash(partTokenChainContent.getJSONArray(i)
                                            .getJSONObject(partTokenChainContent.getJSONArray(i).length() - 1)
                                            .getString("tid"), "SHA3-256"));

                        newPartObject.put("amount", partAmount);
                        newPartObject.put("cheque", chequeHash);
                        newPartObject.put("role", "Receiver");

                        File chainFile = new File(
                                PART_TOKEN_CHAIN_PATH.concat(partTokens.getString(i)).concat(".json"));
                        if (chainFile.exists()) {

                            String readChain = readFile(PART_TOKEN_CHAIN_PATH + partTokens.getString(i) + ".json");
                            JSONArray readChainArray = new JSONArray(readChain);
                            readChainArray.put(partTokenChainContent.getJSONArray(i)
                                    .getJSONObject(partTokenChainContent.getJSONArray(i).length() - 1));
                            readChainArray.put(newPartObject);

                            writeToFile(PART_TOKEN_CHAIN_PATH + partTokens.getString(i) + ".json",
                                    readChainArray.toString(), false);

                        } else {
                            partTokenChainContent.getJSONArray(i).put(newPartObject);
                            writeToFile(PART_TOKEN_CHAIN_PATH + partTokens.getString(i) + ".json",
                                    partTokenChainContent.getJSONArray(i).toString(), false);
                        }
                    }

                    //Delete nft token from NFTSeller once traansfered
                    deleteFile(NFT_TOKENS_PATH+nftTokenIpfsHash);
                    
                    JSONObject rbtTransactionRecord = new JSONObject();
                    rbtTransactionRecord.put("role", "Receiver");
                    rbtTransactionRecord.put("tokens", allTokens);
                    rbtTransactionRecord.put("txn", tid);
                    rbtTransactionRecord.put("quorumList", quorumSignatures.keys());
                    rbtTransactionRecord.put("senderDID", buyerDidIpfsHash);
                    rbtTransactionRecord.put("receiverDID", sellerDidIpfsHash);
                    rbtTransactionRecord.put("Date", getCurrentUtcTime());
                    rbtTransactionRecord.put("totalTime", (endTime - startTime));
                    rbtTransactionRecord.put("comment", comment);
                    rbtTransactionRecord.put("essentialShare", essentialShare);
                    amount = formatAmount(amount);
                    rbtTransactionRecord.put("amount-received", amount);

                    JSONArray transactionHistoryEntry = new JSONArray();
                    transactionHistoryEntry.put(rbtTransactionRecord);
                    updateJSON("add", WALLET_DATA_PATH + "TransactionHistory.json", transactionHistoryEntry.toString());

                    JSONObject nftTransactionRecord = new JSONObject();
                    nftTransactionRecord.put("role", "Seller");
                    nftTransactionRecord.put("tokens", nftTokenIpfsHash);
                    nftTransactionRecord.put("txn", tid);
                    nftTransactionRecord.put("quorumList", nftQuorumSig.keys());
                    nftTransactionRecord.put("senderDID", sellerDidIpfsHash);
                    nftTransactionRecord.put("receiverDID", buyerDidIpfsHash);
                    nftTransactionRecord.put("Date", getCurrentUtcTime());
                    nftTransactionRecord.put("totalTime", (endTime - startTime));
                    nftTransactionRecord.put("comment", comment);
                    nftTransactionRecord.put("essentialShare", essentialShare);
                    amount = formatAmount(amount);
                    // rbtTransactionRecord.put("amount-received", amount);

                    JSONArray nftTransactionHistoryEntry = new JSONArray();
                    nftTransactionHistoryEntry.put(nftTransactionRecord);
                    updateJSON("add", WALLET_DATA_PATH + "nftTransactionHistory.json",
                            nftTransactionHistoryEntry.toString());

                    /*JSONObject nftTokenChainObj = new JSONObject();

                    nftTokenChainObj.put("sellerDID", sellerDidIpfsHash);
                    nftTokenChainObj.put("BuyerDID", buyerDIDFile);
                    nftTokenChainObj.put("sellerSign", );
                    nftTokenChainObj.put("comment", comment);
                    nftTokenChainObj.put("tid", tid);
                    nftTokenChainObj.put("nftToken", nftTokenIpfsHash);
                    nftTokenChainObj.put("RBT-value", amount);*/

                    /*
                     * String nftSellerSign = pvtKeySign(nftTokenChainObj.toString(), sellerPvtKey);
                     * 
                     * output.println(nftSellerSign);
                     */

                    for (int i = 0; i < wholeTokens.length(); i++) {
                        String bankFile = readFile(PAYMENTS_PATH.concat("BNK00.json"));
                        JSONArray bankArray = new JSONArray(bankFile);
                        JSONObject tokenObject1 = new JSONObject();
                        tokenObject1.put("tokenHash", wholeTokens.getString(i));
                        bankArray.put(tokenObject1);
                        Functions.writeToFile(PAYMENTS_PATH.concat("BNK00.json"), bankArray.toString(), false);

                    }

                    String partsFile = readFile(PAYMENTS_PATH.concat("PartsToken.json"));
                    JSONArray partsReadArray = new JSONArray(partsFile);

                    for (int i = 0; i < partTokens.length(); i++) {
                        boolean writeParts = true;
                        for (int j = 0; j < partsReadArray.length(); j++) {
                            if (partsReadArray.getJSONObject(j).getString("tokenHash").equals(partTokens.getString(i)))
                                writeParts = false;
                        }
                        if (writeParts) {
                            JSONObject partObject = new JSONObject();
                            partObject.put("tokenHash", partTokens.getString(i));
                            partsReadArray.put(partObject);
                        }
                    }
                    writeToFile(PAYMENTS_PATH.concat("PartsToken.json"), partsReadArray.toString(), false);

                    nftSellerLogger.info("Transaction ID: " + tid + "NFT Transaction Successful");
                    output.println("Send Response");
                    APIResponse.put("did", sellerDidIpfsHash);
                    APIResponse.put("tid", tid);
                    APIResponse.put("status", "Success");
                    APIResponse.put("tokens", wholeTokens);
                    APIResponse.put("comment", comment);
                    APIResponse.put("message", "NFT Transaction Successful");
                    nftSellerLogger.info("NFT Transaction Successful");
                    IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                    output.close();
                    input.close();
                     
                    sk.close();
                    ss.close();
                    return APIResponse.toString();

                }
                output.println("count mistmatch");
                APIResponse.put("did", sellerDidIpfsHash);
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "count mismacth");
                nftSellerLogger.info(" Transaction failed");
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                output.close();
                input.close();
                 
                sk.close();
                ss.close();
                return APIResponse.toString();

            }

            /*
             * APIResponse.put("did", buyerDidIpfsHash);
             * APIResponse.put("tid", "null");
             * APIResponse.put("status", "Failed");
             * APIResponse.put("message", "Failed to unpin");
             * nftSellerLogger.info(" Transaction failed");
             * IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
             * output.close();
             * input.close();
             * sk.close();
             * ss.close();
             * return APIResponse.toString();
             * 
             * }
             */
            APIResponse.put("did", buyerDidIpfsHash);
            APIResponse.put("tid", "null");
            APIResponse.put("status", "Failed");
            APIResponse.put("message", "Consensus failed at Sender side");
            nftSellerLogger.info(" Transaction failed");
            IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
            output.close();
            input.close();
             
            sk.close();
            ss.close();
            return APIResponse.toString();

        } catch (Exception e) {
            IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
            nftSellerLogger.error("Exception Occurred", e);
            return APIResponse.toString();
        } finally {
            try {
                ss.close();
                sk.close();
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
            } catch (Exception e) {
                IPFSNetwork.executeIPFSCommands(" ipfs p2p close -t /p2p/" + buyerPeerID);
                nftSellerLogger.error("Exception Occurred", e);
            }

        }
    }
}