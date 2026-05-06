package network;

import java.io.*;
import java.net.*;
import java.util.Scanner;

// Chạy file này để giả lập 1 client kết nối đến Server
public class AuctionClient {

    private static final String HOST = "localhost";
    private static final int    PORT = 9999;

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in  = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter    out = new PrintWriter(
                     socket.getOutputStream(), true)) {

            System.out.print("Nhap ten cua ban: ");
            String name = scanner.nextLine();
            out.println(name); // gửi tên lên Server

            // Thread riêng để đọc message từ Server (non-blocking)
            Thread readerThread = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println("[SERVER]: " + msg);
                    }
                } catch (IOException e) {
                    System.out.println("Mat ket noi voi server.");
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            // Vòng lặp gửi lệnh lên Server
            System.out.println("Lenh: BID:<auctionId>:<amount> hoac LIST hoac quit");
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("quit")) break;
                out.println(input);
            }
        }
        System.out.println("Da ngat ket noi.");
    }
}
