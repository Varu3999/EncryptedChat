# Chat Application (Encrypted with Signature) [![java:v8](https://img.shields.io/badge/Java-v8-brightgreen.svg)](https://www.java.com)

This is a Chat Engine built on Java using the Socket Library. It contains a Server which acts as an interface between Client and helps in exchange of messages. This project is a part of Computer Networks course COL 334 taught by Prof. Aaditeshwar Seth.

# Features

  - Multiple Clients can be connected to the Server and can Chat at the same time.
  - Once connected to the Server the Client doesn't need to reconnect to send or receive new messages
  - The Server can run in 3 modes : Non-Encrypted Mode / Encrypted Mode / Encrypted Mode with Signature Check
  - You can set the mode of the server while setting it up and all the clients will connect to it in the same mode automatically
 
### Modes of Operations
- **Non Encrypted** : This is the basic mode of operation in which communication between the clients is not encrypted and anyone who has access to the server can read the messages between any 2 clients. This isn't a secure way of communication as the message can be seen if packet sniffing tools are used.
- **Encrypted** : This is a more secure mode of communication as in this mode the communication between the clients is encrypted so even if someone tries to sniff the packets or has the control over the server, he/she won't be able to see the messages. This encryption is done by generation a private key and public key pair for all client. The public key of each user is stored at the server. Whenever a Client A wants to send a message to Client B, Client A will encrypt the message using the public key of B. This makes sure that the data can only be decrypted by Client B using its private key. The problem with this approach is that if a Client C tries to send a message to Client B using Client A's id, Client B won't be able to detect it and would conclude that the message was sent by Client A.
- **Encrypted with Signature Check** : This is the most secure way of communication as in this mode, while sending the message, Client A also sends a Signature by hashing the encrypted message and encrypting it again using its own private key. 'A' sends both Signature and the encrypted message so that when Client B receives the message, it can verify the user by decrypting the signature using A's public key.
 
 
# How to Use

**Downoload the project in the directory you want**

```sh
$ git clone https://github.com/Varu3999/EncryptedChat
$ cd EncryptedChat
$ cd Combined
```

**Start the Server**

```sh
$ javac TCPServer.java
$ java TCPServer
```

When the server starts, select the mode from the 3 specified modes and press Enter.
The server would start running on your localhost:1234

**Start a Client**

```sh
$ javac Client.java
$ java Client
```

Enter the IP of the server you want your client to connect with. If you are running your client on the same machine, simply put 'localhost'. If you enter an incorrect IP address, the client will try to connect with the server, and after a few seconds, would ask the client to enter the correct IP again. 
Now Select your Username and Start Chatting with your Friends 😄!!!

# Contributors
- Varun Gupta
- Rushang Gupta
