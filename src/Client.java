import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

class Client implements Runnable {

    private Socket clientSocket = null;
    private String name = null;
    private PrintWriter out;


    Client(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }


    public void run() {
        try {

            BufferedReader in = new
                    BufferedReader(new
                    InputStreamReader(clientSocket.getInputStream()));
            out = new
                    PrintWriter(clientSocket.getOutputStream(), true);
            String command = in.readLine();
            String tryName = "";
            if (command.startsWith("registration")) {
                String name = in.readLine();
                String password = in.readLine();
                if (registration(name, password)) {
                    out.println("!");
                } else {
                    out.println("-");
                }
                return;
            } else if (command.startsWith("connect")) {
                tryName = in.readLine();
                String tryPassword = in.readLine();
                if (!checkPassword(tryName, tryPassword)) {
                    out.println("В доступе отказано!");
                    System.out.println("Пользователь " + tryName + " ввел неправильный пароль");
                    out.close();
                    in.close();
                    return;
                }
            }
            this.name = tryName;
            System.out.println("Подключился " + name);
            if (!VFServer.clients.contains(this)) {
                for (Client client : VFServer.clients) {
                    out.println(client.name);
                }
                out.println("!");

                printToClients("+" + this.name);
                VFServer.clients.add(this);
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("?") && !VFServer.alreadyUsed) {
                        VFServer.alreadyUsed = true;
                        VFServer.activeUserName = this.name;
                        VFServer.messageLength = Integer.valueOf(line.substring(1));
                        System.out.println(VFServer.index);
                        out.println("START" + VFServer.index);
                        printToClients("*" + VFServer.index);
                        out.println("*" + VFServer.index);
                    } else if (VFServer.alreadyUsed && this.name.equals(VFServer.activeUserName) && line.startsWith("*" + VFServer.index + "*")) {
                        VFServer.roundCount.getAndAdd(1);
                        while (VFServer.roundCount.get() != VFServer.clients.size()) {
                            System.out.println(VFServer.roundCount);
                            Thread.sleep(100);
                        }
                        synchronized (VFServer.lock) {
                            VFServer.roundResult.getAndAdd(Integer.valueOf(line.substring(line.indexOf("*", 1) + 1)));
                        }
                        VFServer.results.add(VFServer.index, new AtomicInteger(VFServer.roundResult.get() % 2));
                        if ((VFServer.index + 1) % 16 == 0 && VFServer.index != 0) {
                            /*int indexOfChar = VFServer.results.get(VFServer.index) * 16 +
                                    VFServer.results.get(VFServer.index - 1) * 8 +
                                    VFServer.results.get(VFServer.index - 2) * 4 +
                                    VFServer.results.get(VFServer.index - 3) * 2 +
                                    VFServer.results.get(VFServer.index - 4);
                            char c = VFServer.alphabet.charAt(indexOfChar);*/
                            int asciiCode = 0;
                            int powerOf2 = 1;
                            for (int i = 0; i < 16; i++) {
                                asciiCode += VFServer.results.get(VFServer.index - i).get() * powerOf2;
                                powerOf2 *= 2;
                            }
                            char c = (char) asciiCode;
                            System.out.println(asciiCode);
                            printToClients("|" + c);
                            out.println("|" + c);
                        }
                        VFServer.index++;
                        VFServer.roundCount.set(0);
                        VFServer.roundResult.set(0);
                        VFServer.messageLength--;
                        if (VFServer.messageLength == 0) {
                            VFServer.alreadyUsed = false;
                            System.out.println(name + ": " + line);
                            continue;
                        }

                        printToClients("*" + VFServer.index);
                        out.println("*" + VFServer.index);
                    } else if (line.startsWith("*" + VFServer.index + "*")) {
                        VFServer.roundCount.getAndAdd(1);
                        VFServer.roundResult.getAndAdd(Integer.valueOf(line.substring(line.indexOf("*", 1) + 1)));
                    }

                    System.out.println(name + ": " + line);

                }
                VFServer.clients.remove(this);
                printToClients("-" + this.name);
                System.out.println("Отключился " + name);
            } else {
                out.println("Ошибка! Пользователь с таким именем уже подключен к серверу");
            }

            out.close();
            in.close();

        } catch (SocketException e) {
            VFServer.clients.remove(this);
            printToClients("-" + this.name);
            System.out.println("Отключился " + name);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private PrintWriter getOut() {
        return out;
    }

    private void printToClients(String s) {
        VFServer.clients.stream().filter(client -> client != this).forEach(client -> client.getOut().println(s));
    }

    public boolean checkPassword(String login, String password) throws FileNotFoundException {
        Scanner passwordScanner = new Scanner(new File("passwords.txt"));
        while (passwordScanner.hasNextLine()) {
            String line = passwordScanner.nextLine();
            String[] splitedLine = line.split("\"");
            if (login.equals(splitedLine[1])) {
                if (password.equals(splitedLine[3])) {
                    return true;
                } else {
                    return false;
                }
            } else {
                continue;
            }
        }
        return false;
    }

    public boolean registration(String login, String password) throws IOException {
        Scanner passwordScanner = new Scanner(new File("passwords.txt"));
        while (passwordScanner.hasNextLine()) {
            String line = passwordScanner.nextLine();
            String[] splitedLine = line.split("\"");
            if (login.equals(splitedLine[1])) {
                return false;
            }
        }
        String text = "\"" + login + "\" " + "\"" + password + "\"" + "\n";
        Files.write(Paths.get("passwords.txt"), text.getBytes(), StandardOpenOption.APPEND);
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Client client = (Client) o;

        return name != null ? name.equals(client.name) : client.name == null;

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}