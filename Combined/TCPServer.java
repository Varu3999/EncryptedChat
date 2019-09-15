import java.io.*;
import java.net.*;
import java.util.*;
import javafx.util.Pair;

class TCPServer 
{
	Hashtable<String, Pair<String, Socket[]>> map;
    ServerSocket welcomeSocket;
    String run_mode;
	TCPServer(int port_number)
	{
		try
		{
            welcomeSocket = new ServerSocket(port_number);
			map = new Hashtable<>();
		}
		catch(Exception e)
		{
			System.out.println(e);
		}		
	}
	
    public static void main(String argv[]) throws Exception
    {        
        TCPServer wa_server = new TCPServer(1234);
        wa_server.getMode();
        while(true)
        {   
            Socket conn_socket = wa_server.welcomeSocket.accept();
            ServerThread st= new ServerThread(conn_socket, wa_server, wa_server.run_mode);
            st.start();
        }
    }

    public void getMode(){
        try{
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Please select a mode from the following 3 modes:\n" +
                             "1. Non-Encrypted\n" +
                             "2. Encrypted\n" +
                             "3. Encrypted with Signature\n" +
                             "Mode:\n");
            run_mode = inFromUser.readLine();
            if(!run_mode.equals("1") && !run_mode.equals("2") && !run_mode.equals("3"))
            {
                System.out.println("Please enter a value between 1 & 3");
                getMode();
            }
        }catch(Exception e){
            System.out.println("Please enter a value between 1 &  hjjyfy3");
            getMode();
        }        
    }
}


class ServerThread extends Thread
{
    Socket socket;
    String my_name;
    String mode;
    Hashtable<String, Pair<String, Socket[]>> user_info;

    ServerThread(Socket socket, TCPServer wa_server, String run_mode)
    {
        this.socket = socket;
        user_info = wa_server.map;
        mode = run_mode;
    }

    public void run()
    {        
        try
        {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream  outToClient = new DataOutputStream(socket.getOutputStream());
            while(true)
            {
    
                String serverSentence = "error";
                String clientSentence;
                
                clientSentence = inFromClient.readLine();
                String[] split_clientSentence = clientSentence.split(" ");

                if(split_clientSentence[0].equals("REGISTER"))
                {                    
                    if(split_clientSentence.length>5)
                    {
                        serverSentence = "ERROR 100 Malformed username\n";
                        outToClient.writeBytes(serverSentence);
                    }
                    if(split_clientSentence[1].equals("TOSEND"))
                    {                        
                        String username = split_clientSentence[2];
                        my_name = username;
                        if(isCorrectUsername(username) && (mode.equals("2") || mode.equals("3")))
                        {
                            serverSentence = "REGISTERED TOSEND " + username +"\n\n";
                            Socket[] sockets = new Socket[2];
                            sockets[0] = socket;
                            int key_length =  Integer.parseInt(inFromClient.readLine());                           
                            String public_key = "";
                            int value = 0;
         
                            while(key_length!=0) 
                            {
                                value = inFromClient.read();
                                char c = (char)value;
                                public_key+=c;
                                key_length-=1;
                            }                   
                            Pair<String, Socket[]> p = new Pair<String, Socket[]>(public_key, sockets);
                            user_info.put(username, p);
                            outToClient.writeBytes(serverSentence);                     
                        }
                        else if(isCorrectUsername(username))
                        {
                            serverSentence = "REGISTERED TOSEND " + username +"\n\n";
                            // Username and sockets stored in a Hashmap
                            // {username, rec_socket}
                            Socket[] sockets = new Socket[2];
                            sockets[0] = socket;
                            Pair<String, Socket[]> p = new Pair<String, Socket[]>(username, sockets);
                            user_info.put(username, p);
                            outToClient.writeBytes(serverSentence);
                        }
                        else
                        {
                            serverSentence = "ERROR 100 Malformed username\n\n";
                            outToClient.writeBytes(serverSentence);
                        }
                    }
                    else if(split_clientSentence[1].equals("TORECV"))
                    {                        
                        inFromClient.readLine();
                        String username = split_clientSentence[2];
                        if(isCorrectUsername(username))
                        {
                            serverSentence = "REGISTERED TORECV " + username +"\n\n";                            
                            Socket[] sockets1 = user_info.get(username).getValue();                            
                            sockets1[1] = socket;  
                            outToClient.writeBytes(serverSentence);
                            this.stop();                                            
                        }
                        else
                        {
                            serverSentence = "ERROR 100 Malformed username\n";
                            outToClient.writeBytes(serverSentence);
                        }                        
                    }                               
                }
                else if(split_clientSentence[0].equals("GETMODE"))
                {
                    inFromClient.readLine();
                    outToClient.writeBytes("MODEIS "+mode+"\n\n");
                }
                else if(split_clientSentence[0].equals("SEND"))
                {
                    // Reads the username from the message
                    String user_to_send = split_clientSentence[1];
                    //System.out.println("Sending message to " + user_to_send);

                    // Reads the content length from the message
                    clientSentence = inFromClient.readLine();
                    int content_length;
                    split_clientSentence = clientSentence.split(": ");
                    content_length = Integer.parseInt(split_clientSentence[1]);

                    // Reads the message from the client
                    inFromClient.readLine();
                                        
                    try
                    {
                        // Finds the username from the map formed                        
                        if(user_info.get(user_to_send)!=null)
                        {
                            Socket[] sockets11 = user_info.get(user_to_send).getValue();
                            Socket rec_socket_rec = sockets11[1];
                            String rec_sentence;                        
                            DataOutputStream outToRecp = new DataOutputStream(rec_socket_rec.getOutputStream());
                            if(mode.equals("3"))
                            {
                                String sending_message = inFromClient.readLine();
                                String sign_hash = inFromClient.readLine();
                                inFromClient.readLine();
                                outToRecp.writeBytes("FORWARD " + my_name + "\n" 
                                                + sign_hash + "\n" 
                                                + user_info.get(my_name).getKey() + "\n"
                                                + "Content-length: " + content_length + "\n\n"
                                                + sending_message);                                               
                            }
                            else if(mode.equals("2"))
                            {                                
                                String sending_message = "";
                                int value = 0;      
                                int buff_len = content_length;      
                                while(buff_len!=0) {
                                    value = inFromClient.read();
                                    char c = (char)value;
                                    sending_message+=c;
                                    buff_len-=1;
                                }
                                outToRecp.writeBytes("FORWARD " + my_name + "\n"
                                                 + "Content-length: " 
                                                 + content_length + "\n\n" 
                                                 + sending_message);                                                 
                            }
                            else
                            {
                                String sending_message = "";
                                int value = 0;      
                                int buff_len = content_length;      
                                while(buff_len!=0) {
                                    value = inFromClient.read();
                                    char c = (char)value;
                                    sending_message+=c;
                                    buff_len-=1;
                                }
                                outToRecp.writeBytes("FORWARD " + my_name + "\n" + "Content-length: " + content_length + "\n\n" + sending_message);
                            }
                            BufferedReader inFromRecp = new BufferedReader(new InputStreamReader(sockets11[1].getInputStream()));                          
                            rec_sentence = inFromRecp.readLine();
                            inFromRecp.readLine();
                            if(rec_sentence.equals("RECEIVED " + my_name))
                            {
                                outToClient.writeBytes("SENT " + user_to_send + "\n\n");
                            }  
                        }
                        else
                        {
                            outToClient.writeBytes("USER NOT FOUND\n\n");
                            System.out.println("USER NOT FOUND\n\n");
                        }                               
                    }
                    catch(Exception e)
                    {
                        outToClient.writeBytes("ERROR 101 UNABLE TO SEND\n\n");
                    }
                }
                else if(split_clientSentence[0].equals("FETCHKEY"))
                {
                    inFromClient.readLine();
                    String user_of_key = split_clientSentence[1];
                    if(user_info.get(user_of_key)!=null)
                    {
                        String key_of_user = user_info.get(user_of_key).getKey();
                        outToClient.writeBytes("KEYIS\n"
                                            + key_of_user.length() 
                                            + "\n"+key_of_user);
                    } 
                    else
                    {
                        outToClient.writeBytes("USER NOT FOUND\n\n");
                    }                    
                }
                else
                {
                    outToClient.writeBytes("USER NOT FOUND\n\n");
                }
            }
        }
        catch(Exception e)
        {
            // System.out.println(e);
            try{
                user_info.remove(my_name);
                System.out.println("DEREGISTERED " + my_name);            
            }catch(Exception p){
                int a = 1;
            }
            this.stop();
        }
    }

    public Boolean isCorrectUsername(String username)
    {
        Boolean f = username.matches("[a-zA-Z0-9]+");
        if(f)
        {
            if(user_info.contains(username))
            {
                f = false;
            }
        }
        return f;
    }
}