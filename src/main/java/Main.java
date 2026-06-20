import java.util.Scanner;
import java.io.File;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            String input = scanner.nextLine();

            if (input.equals("exit")) {
                break;
            } else if (input.equals("pwd")) {
                System.out.println(Paths.get("").toAbsolutePath().normalize());

            } else if (input.startsWith("echo ")) {
                System.out.println(input.substring(5));

            } else if (input.startsWith("type ")) {
                String command = input.substring(5);

                if (command.equals("echo")
                        || command.equals("exit")
                        || command.equals("type")
                        || command.equals("pwd")) {

                    System.out.println(command + " is a shell builtin");

                } else {
                    String pathEnv = System.getenv("PATH");
                    String[] paths = pathEnv.split(File.pathSeparator);

                    boolean found = false;

                    for (String path : paths) {
                        File file = new File(path, command);

                        if (file.exists() && file.canExecute()) {
                            System.out.println(command + " is " + file.getAbsolutePath());
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        System.out.println(command + ": not found");
                    }
                }

            } else {
                String[] parts = input.split(" ");
                String command = parts[0];

                String pathEnv = System.getenv("PATH");
                String[] paths = pathEnv.split(File.pathSeparator);

                File executable = null;

                for (String path : paths) {
                    File file = new File(path, command);

                    if (file.exists() && file.canExecute()) {
                        executable = file;
                        break;
                    }
                }

                if (executable != null) {
                    ProcessBuilder pb = new ProcessBuilder(parts);
                    pb.inheritIO();

                    Process process = pb.start();
                    process.waitFor();
                } else {
                    System.out.println(input + ": command not found");
                }
            }
        }
    }
}