import java.security.KeyFactory;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Base64;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

import java.security.MessageDigest;

import java.io.*;
import java.net.*;

class Client {

    public static void main(String argv[]) 
    {
        try{
            String hostIP;
            String port = "1234";
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Server IP: ");
            hostIP = inFromUser.readLine();
            //System.out.println("write correct IP");
            Socket clientSocket = new Socket(hostIP, Integer.parseInt(port));
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outToServer.writeBytes("GETMODE\n\n");
            
            String response = inFromServer.readLine();
            
            //clientSocket.close();
            String splitRes[] = response.split(" ");
            if(splitRes[0].equals("MODEIS")){
                String input = splitRes[1];
                if(input.equals("1")){
                    Client_Non_Encrypted cl = new Client_Non_Encrypted();
                    String a[] = new String[1];
                    a[0] = hostIP;
                    cl.main(a);
                }else if(input.equals("2")){
                    Client_Encrypted cl = new Client_Encrypted();;
                    String a[] = new String[1];
                    a[0] = hostIP;
                    cl.main(a);
                }else if(input.equals("3")){
                    Client_Signature cl = new Client_Signature();
                    String a[] = new String[1];
                    a[0] = hostIP;
                    cl.main(a);
                }else{
                    System.out.println("Server does not exist. Please try again!");
                    String a[] = new String[1];
                    a[0] = hostIP;
                    main(a);
                }
            }else{
                System.out.println("Server does not exist. Please try again!");
                String a[] = new String[1];
                main(a);
            }
        }catch(Exception e){
            System.out.println("Server does not exist. Please try again!");
            String a[] = new String[1];
            main(a);
        }
    }
}

class Client_Signature {
    
    private static final String ALGORITHM = "RSA";
    public static int port = 1234;
    public String userName = "";
    public static String hostIP = "";
    public Socket clientSocketSen;
    public Socket clientSocketRec;
    public byte[] publicKey;
    public byte[] privateKey;

    public static void main(String argv[]) 
    {
        try{
            Client_Signature ob = new Client_Signature();
            ob.hostIP = argv[0];
            ob.generateKeyPair();
            ob.registerToSend();
            ob.registerToReceive();
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            Receiver_Signature rec = new Receiver_Signature(ob.clientSocketRec , ob.privateKey);
            rec.start();
            System.out.println("You can send message by typing @(username) (message) and press enter");
            String message = "";
            String to = "";
            while(true){    
                String msg = inFromUser.readLine();
                if(msg.charAt(0) == '@'){
                    
                        if(!msg.contains(" ")){
                            System.out.println("WRONG FORMAT!!");
                            continue;
                        }
                        String[] msgSplit = msg.split(" ",2);
                        if(msgSplit[1].length() > 0 && msgSplit[0].length() > 0){
                            message = msgSplit[1];
                            msgSplit = msgSplit[0].split("@",2);
                            to = msgSplit[1];
                            ob.sendMessage(to , message);
                        }else{
                            System.out.println("WRONG FORMAT!!");
                        }
                    
                }else{
                    System.out.println("WRONG FORMAT!!");
                }
            }
        }catch(Exception e){
            //System.out.println("Server is DOWN!!!!!!!");
            //System.out.println("Try another Server IP!!");
            String a[] = new String[1];        
            main(a);
        }
        
    }


    private void sendMessage(String to , String message) throws Exception
    {
        DataOutputStream outToServer = new DataOutputStream(clientSocketSen.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketSen.getInputStream()));
        outToServer.writeBytes("FETCHKEY " + to + "\n\n");
        String response = inFromServer.readLine();
        if(!response.equals("KEYIS")){
            System.out.println("User doesn't Exist!!!");
            return;
        }
        try{
            response = inFromServer.readLine();
            //System.out.println(response);
            int keyLen = Integer.parseInt(response);
            int value = 0;
            response = "";
            while(keyLen!=0) 
            {
                value = inFromServer.read();
                char c = (char)value;
                response+=c;
                keyLen-=1;
            }
            byte[] encryptMsg = encrypt(Base64.getDecoder().decode(response), message.getBytes());
            String messagee = Base64.getEncoder().encodeToString(encryptMsg);
            
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] shaBytes = md.digest(encryptMsg);
            encryptMsg = encryptpriv(privateKey, shaBytes);
            String encodedHash = Base64.getEncoder().encodeToString(encryptMsg);
            //System.out.println(encodedHash);
            outToServer.writeBytes("SEND " + to + "\nContent-length: " + messagee.length() + "\n\n" + messagee + "\n" + encodedHash + "\n\n");
            // outToServer.writeBytes("SEND " + to);
            response = inFromServer.readLine();
            String[] splitRes = response.split(" ");
            if(!splitRes[0].equals("SENT")){
                if(splitRes[0].equals("ERROR") && splitRes[1] == "102"){
                    System.out.println("Unable To Send ....");
                }else{
                    System.out.println("Header Incomplete ....");
                }
            }else{
                System.out.println("Message Sent Successfully ....!!");
            }    
        }catch(Exception e){
            //System.out.println(e);
        }
        
    }

    private void registerToSend() throws Exception
    {
        
        clientSocketSen = new Socket(hostIP, port);
        System.out.print("User Name: ");
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream outToServer = new DataOutputStream(clientSocketSen.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketSen.getInputStream()));
        userName = inFromUser.readLine();                       
        String publicKeyB = Base64.getEncoder().encodeToString(publicKey);
        outToServer.writeBytes("REGISTER TOSEND " + userName + "\n" + publicKeyB.length() + "\n"+ publicKeyB);
        String response = inFromServer.readLine();
        String[] splitRes = response.split(" ");
        inFromServer.readLine();
        if(!(splitRes[0].equals("REGISTERED") && splitRes[1].equals("TOSEND") && splitRes[2].equals(userName))){
            System.out.println("NOT A VALID USER NAME OR USERNAME ALREADY REGISTERED!!!");
            registerToSend();
        }   
    }

    private void registerToReceive() throws Exception
    {
        clientSocketRec = new Socket(hostIP, port);
        DataOutputStream outToServer = new DataOutputStream(clientSocketRec.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketRec.getInputStream()));
        outToServer.writeBytes("REGISTER TORECV " + userName + "\n\n");
        String response = inFromServer.readLine();
        String[] splitRes = response.split(" ");
        inFromServer.readLine();
        if(!(splitRes[0].equals("REGISTERED") && splitRes[1].equals("TORECV") && splitRes[2].equals(userName))){
            clientSocketRec.close();
            registerToReceive();
        }
    }
    public void generateKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

        // 512 is keysize
        keyGen.initialize(512, random);

        KeyPair generateKeyPair = keyGen.generateKeyPair();
        
        publicKey = generateKeyPair.getPublic().getEncoded();
        privateKey = generateKeyPair.getPrivate().getEncoded();
    }

    public byte[] encrypt(byte[] publicKey, byte[] inputData)
            throws Exception {
        PublicKey key = KeyFactory.getInstance(ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(publicKey));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedBytes = cipher.doFinal(inputData);

        return encryptedBytes;
    }

    public byte[] encryptpriv(byte[] privateKey, byte[] inputData)
            throws Exception {
            PrivateKey key = KeyFactory.getInstance(ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedBytes = cipher.doFinal(inputData);

        return encryptedBytes;
    }

    public byte[] decrypt(byte[] privateKey, byte[] inputData)
            throws Exception {

        PrivateKey key = KeyFactory.getInstance(ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedBytes = cipher.doFinal(inputData);

        return decryptedBytes;
    }

}

class Receiver_Signature extends Thread{
    public Socket socketRec;
    public BufferedReader inFromServer;
    private static final String ALGORITHM = "RSA";
    DataOutputStream outToServer;
    public byte[] privateKey;
    public Receiver_Signature(Socket socket , byte[] key) throws Exception{
        socketRec = socket;
        privateKey = key;
        inFromServer = new BufferedReader(new InputStreamReader(socketRec.getInputStream()));
        outToServer = new DataOutputStream(socketRec.getOutputStream());
    }
    @Override
    public void run(){
        try{
            while(true){
                try{
                    String finalMsg = "";
                    String sender = "";
                    String response = inFromServer.readLine();
                    String[] splitRes = response.split(" ");
                    finalMsg += splitRes[1];
                    sender = splitRes[1];
                    response = inFromServer.readLine();
                    String hashMsg = response;
                    response = inFromServer.readLine();
                    String publicKeySender = response;
                    //System.out.println(hashMsg);
                    byte[] senderMsg = decryptpub(Base64.getDecoder().decode(publicKeySender),Base64.getDecoder().decode(hashMsg));
                   
                    response = inFromServer.readLine();
                    splitRes = response.split(": ");
                    int contentLength = Integer.parseInt(splitRes[1]);
                    response = inFromServer.readLine();
                    response = "";
                    int value = 0;
                    while(contentLength!=0) 
                    {
                        value = inFromServer.read();
                        char c = (char)value;
                        response+=c;
                        contentLength-=1;
                    }
                    byte[] msg;
                    msg = Base64.getDecoder().decode(response);
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] msgRecB = md.digest(msg);
                    msg = decrypt(privateKey, msg);
                    String msgRec = new String(msg);
                    // byte[] msgRecB = Base64.getDecoder().decode(msgRec);
                    if(Arrays.equals(msgRecB, senderMsg)){
                        finalMsg += ": " + msgRec;
                        System.out.println(finalMsg);
                    }
                    else{
                        System.out.println("User not authenticated!!!!!");
                    }
                    
                    outToServer.writeBytes("RECEIVED " + sender + "\n\n");
                    
                }catch(Exception e){
                    //System.out.println(e);
                    
                    System.out.println("Server Is Down");
                    System.exit(0);
                    break;
                }   
            }
        }
        catch(Exception e){
            System.out.println("hi");
        }

    }
    public byte[] decrypt(byte[] privateKey, byte[] inputData)
            throws Exception {

        PrivateKey key = KeyFactory.getInstance(ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedBytes = cipher.doFinal(inputData);

        return decryptedBytes;
    }
    
    public byte[] decryptpub(byte[] publicKey, byte[] inputData)
            throws Exception {

            PublicKey key = KeyFactory.getInstance(ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(publicKey));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedBytes = cipher.doFinal(inputData);

        return decryptedBytes;
    }
}


class Client_Encrypted {
    
    private static final String ALGORITHM = "RSA";
    public static int port = 1234;
    public String userName = "";
    public static String hostIP = "";
    public Socket clientSocketSen;
    public Socket clientSocketRec;
    public byte[] publicKey;
    public byte[] privateKey;

    public static void main(String argv[]) 
    {
        try{
            Client_Encrypted ob = new Client_Encrypted();
            ob.hostIP = argv[0];
            ob.generateKeyPair();
            ob.registerToSend();
            ob.registerToReceive();
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            Receiver_Encrypted rec = new Receiver_Encrypted(ob.clientSocketRec , ob.privateKey);
            rec.start();
            System.out.println("You can send message by typing @(username) (message) and press enter");
            String message = "";
            String to = "";
            while(true){    
                String msg = inFromUser.readLine();
                if(msg.charAt(0) == '@'){
                    
                        if(!msg.contains(" ")){
                            System.out.println("WRONG FORMAT!!");
                            continue;
                        }
                        String[] msgSplit = msg.split(" ",2);
                        if(msgSplit[1].length() > 0 && msgSplit[0].length() > 0){
                            message = msgSplit[1];
                            msgSplit = msgSplit[0].split("@",2);
                            to = msgSplit[1];
                            ob.sendMessage(to , message);
                        }else{
                            System.out.println("WRONG FORMAT!!");
                        }
                    
                }else{
                    System.out.println("WRONG FORMAT!!");
                }
            }
        }catch(Exception e){
            //System.out.println("Server is DOWN!!!!!!!");
            //System.out.println("Try another Server IP!!");
            String a[] = new String[1];        
            main(a);
        }
        
    }


 

    private void sendMessage(String to , String message) throws Exception
    {
        DataOutputStream outToServer = new DataOutputStream(clientSocketSen.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketSen.getInputStream()));
        outToServer.writeBytes("FETCHKEY " + to + "\n\n");
        String response = inFromServer.readLine();
        if(!response.equals("KEYIS")){
            System.out.println("User doesn't Exist!!!");
            return;
        }
        try{
            response = inFromServer.readLine();
            int keyLen = Integer.parseInt(response);
            int value = 0;
            response = "";
            while(keyLen!=0) 
            {
                value = inFromServer.read();
                char c = (char)value;
                response+=c;
                keyLen-=1;
            }
            byte[] encrytpMsg = encrypt(Base64.getDecoder().decode(response), message.getBytes());
            message = Base64.getEncoder().encodeToString(encrytpMsg);
            outToServer.writeBytes("SEND " + to + "\nContent-length: " + message.length() + "\n\n" + message);
            // outToServer.writeBytes("SEND " + to);
            response = inFromServer.readLine();
            String[] splitRes = response.split(" ");
            if(!splitRes[0].equals("SENT")){
                if(splitRes[0].equals("ERROR") && splitRes[1] == "102"){
                    System.out.println("Unable To Send ....");
                }else{
                    System.out.println("Header Incomplete ....");
                }
            }else{
                System.out.println("Message Sent Successfully ....!!");
            }    
        }catch(Exception e){
            //System.out.println(e);
        }
        
    }

    private void registerToSend() throws Exception
    {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        clientSocketSen = new Socket(hostIP, port);
        System.out.println("User Name:");
        DataOutputStream outToServer = new DataOutputStream(clientSocketSen.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketSen.getInputStream()));
        userName = inFromUser.readLine();                       
        String publicKeyB = Base64.getEncoder().encodeToString(publicKey);
        outToServer.writeBytes("REGISTER TOSEND " + userName + "\n" + publicKeyB.length() + "\n"+ publicKeyB);
        String response = inFromServer.readLine();
        String[] splitRes = response.split(" ");
        if(!(splitRes[0].equals("REGISTERED") && splitRes[1].equals("TOSEND") && splitRes[2].equals(userName))){
            System.out.println("NOT A VALID USER NAME OR USERNAME ALREADY REGISTERED!!!");
            registerToSend();   
        }
    }

    private void registerToReceive() throws Exception
    {
        clientSocketRec = new Socket(hostIP, port);
        DataOutputStream outToServer = new DataOutputStream(clientSocketRec.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketRec.getInputStream()));
        outToServer.writeBytes("REGISTER TORECV " + userName + "\n\n");
        String response = inFromServer.readLine();
        String[] splitRes = response.split(" ");
        if(!(splitRes[0].equals("REGISTERED") && splitRes[1].equals("TORECV") && splitRes[2].equals(userName))){
            clientSocketRec.close();
            registerToReceive();
        }
    }
    public void generateKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

        // 512 is keysize
        keyGen.initialize(512, random);

        KeyPair generateKeyPair = keyGen.generateKeyPair();
        
        publicKey = generateKeyPair.getPublic().getEncoded();
        privateKey = generateKeyPair.getPrivate().getEncoded();
    }

    public byte[] encrypt(byte[] publicKey, byte[] inputData)
            throws Exception {
        PublicKey key = KeyFactory.getInstance(ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(publicKey));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedBytes = cipher.doFinal(inputData);

        return encryptedBytes;
    }

    public byte[] decrypt(byte[] privateKey, byte[] inputData)
            throws Exception {

        PrivateKey key = KeyFactory.getInstance(ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedBytes = cipher.doFinal(inputData);

        return decryptedBytes;
    }

}

class Receiver_Encrypted extends Thread{
    public Socket socketRec;
    public BufferedReader inFromServer;
    private static final String ALGORITHM = "RSA";
    DataOutputStream outToServer;
    public byte[] privateKey;
    public Receiver_Encrypted(Socket socket , byte[] key) throws Exception{
        socketRec = socket;
        privateKey = key;
        inFromServer = new BufferedReader(new InputStreamReader(socketRec.getInputStream()));
        outToServer = new DataOutputStream(socketRec.getOutputStream());
    }
    @Override
    public void run(){
        try{
            while(true){
                try{
                    String finalMsg = "";
                    String sender = "";
                    String response = inFromServer.readLine();
                    
                    String[] splitRes = response.split(" ");
                    finalMsg += splitRes[1];
                    sender = splitRes[1];
                    response = inFromServer.readLine();
                    splitRes = response.split(": ");
                    int contentLength = Integer.parseInt(splitRes[1]);
                    response = inFromServer.readLine();
                    response = "";
                    int value = 0;
                    while(contentLength!=0) 
                    {
                        value = inFromServer.read();
                        char c = (char)value;
                        response+=c;
                        contentLength-=1;
                    }
                    byte[] msg;
                    msg = Base64.getDecoder().decode(response);
                    msg = decrypt(privateKey, msg);
                    response = new String(msg);
                   
                    finalMsg += ": " + response;
                    outToServer.writeBytes("RECEIVED " + sender + "\n\n");
                    System.out.println(finalMsg);
                }catch(Exception e){
                    System.out.println(e.getStackTrace());
                    System.out.println("Server Is Down");
                    System.exit(0);
                    break;
                }   
            }
        }
        catch(Exception e){
            System.out.println("hi");
        }

    }
    public byte[] decrypt(byte[] privateKey, byte[] inputData)
            throws Exception {

        PrivateKey key = KeyFactory.getInstance(ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedBytes = cipher.doFinal(inputData);

        return decryptedBytes;
    }
}

class Client_Non_Encrypted {    
    public static int port = 1234;
    public String userName = "";
    public static String hostIP = "localhost";
    public Socket clientSocketSen;
    public Socket clientSocketRec;
    
    public static void main(String argv[]) 
    {
        try{
            Client_Non_Encrypted ob = new Client_Non_Encrypted();
            ob.hostIP = argv[0];
            ob.registerToSend();
            ob.registerToReceive();
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            Receiver_Non_Encrypted rec = new Receiver_Non_Encrypted(ob.clientSocketRec);
            rec.start();
            System.out.println("You can send message by typing @(username) (message) and press enter");
            String message = "";
            String to = "";
            while(true){    
                String msg = inFromUser.readLine();
                if(msg.charAt(0) == '@'){
                    
                        if(!msg.contains(" ")){
                            System.out.println("WRONG FORMAT!!");
                            continue;
                        }
                        String[] msgSplit = msg.split(" ",2);
                        if(msgSplit[1].length() > 0 && msgSplit[0].length() > 0){
                            message = msgSplit[1];
                            msgSplit = msgSplit[0].split("@",2);
                            to = msgSplit[1];
                            ob.sendMessage(to , message);
                        }else{
                            System.out.println("WRONG FORMAT!!");
                        }
                    
                }else{
                    System.out.println("WRONG FORMAT!!");
                }
            }
        }catch(Exception e){
            //System.out.println("Server is DOWN!!!!!!!");
            //System.out.println("Try another Server IP!!");
            String a[] = new String[1];        
            main(a);
        }
        
    }


    private void sendMessage(String to , String message) throws Exception
    {
        DataOutputStream outToServer = new DataOutputStream(clientSocketSen.getOutputStream());
        outToServer.writeBytes("SEND " + to + "\nContent-length: " + message.length() + "\n\n" + message);
        // outToServer.writeBytes("SEND " + to);
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketSen.getInputStream()));
        String response = inFromServer.readLine();
        String[] splitRes = response.split(" ");
        if(!splitRes[0].equals("SENT")){
            if(splitRes[0].equals("ERROR") && splitRes[1] == "102"){
                System.out.println("Unable To Send ....");
            }else{
                System.out.println("Header Incomplete ....");
            }
        }else{
            System.out.println("Message Sent Successfully ....!!");
        }
    }

    private void registerToSend() throws Exception
    {

        
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        clientSocketSen = new Socket(hostIP, port);
        System.out.println("User Name:");
        DataOutputStream outToServer = new DataOutputStream(clientSocketSen.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketSen.getInputStream()));
        userName = inFromUser.readLine();
        
        outToServer.writeBytes("REGISTER TOSEND " + userName + "\n\n");
        String response = inFromServer.readLine();
        String[] splitRes = response.split(" ");

        if(!(splitRes[0].equals("REGISTERED") && splitRes[1].equals("TOSEND") && splitRes[2].equals(userName))){
            System.out.println("NOT A VALID USER NAME OR USERNAME ALREADY REGISTERED!!!");
            clientSocketSen.close();
            registerToSend();
        }

    }

    private void registerToReceive() throws Exception
    {
        clientSocketRec = new Socket(hostIP, port);
        DataOutputStream outToServer = new DataOutputStream(clientSocketRec.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketRec.getInputStream()));
        outToServer.writeBytes("REGISTER TORECV " + userName + "\n\n");
        String response = inFromServer.readLine();
        String[] splitRes = response.split(" ");
        if(!(splitRes[0].equals("REGISTERED") && splitRes[1].equals("TORECV") && splitRes[2].equals(userName))){
            clientSocketRec.close();
            registerToReceive();
        }
    }
    

}

class Receiver_Non_Encrypted extends Thread{
    public Socket socketRec;
    public BufferedReader inFromServer;
    DataOutputStream outToServer;
    public Receiver_Non_Encrypted(Socket socket) throws Exception{
        socketRec = socket;
        inFromServer = new BufferedReader(new InputStreamReader(socketRec.getInputStream()));
        outToServer = new DataOutputStream(socketRec.getOutputStream());
    }
    @Override
    public void run(){
        try{
            while(true){
                try{
                    String finalMsg = "";
                    String sender = "";
                    String response = inFromServer.readLine();
                    
                    String[] splitRes = response.split(" ");
                    finalMsg += splitRes[1];
                    sender = splitRes[1];
                    response = inFromServer.readLine();
                    splitRes = response.split(": ");
                    int contentLength = Integer.parseInt(splitRes[1]);
                    response = inFromServer.readLine();
                    char[] message = new char[contentLength];;
                    inFromServer.read(message , 0 , contentLength);
                    response = String.valueOf(message);
                    finalMsg += ": " + response;
                    outToServer.writeBytes("RECEIVED " + sender + "\n\n");
                    System.out.println(finalMsg);
                }catch(Exception e){
                    //outToServer.writeBytes("ERROR 103 Header incomplete\n\n");
                    System.out.println("Server Is Down");
                    System.exit(0);
                    break;
                }   
            }
        }
        catch(Exception e){
            System.out.println("hi");
        }

    }
}