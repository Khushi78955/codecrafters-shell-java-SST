import java.util.Scanner;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static File currentDirectory = new File(System.getProperty("user.dir"));

    private static List<String> parseCommand(String input) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            if (inDoubleQuotes && ch == '\\') {
                if (i + 1 < input.length()) {
                    char next = input.charAt(i + 1);

                    if (next == '"' || next == '\\') {
                        current.append(next);
                        i++;
                    } else {
                        current.append('\\');
                        current.append(next);
                        i++;
                    }
                    continue;
                }
            }

            if (!inSingleQuotes && !inDoubleQuotes && ch == '\\') {
                if (i + 1 < input.length()) {
                    current.append(input.charAt(i + 1));
                    i++;
                }
                continue;
            }

            if (ch == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                continue;
            }

            if (ch == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                continue;
            }

            if (Character.isWhitespace(ch)
                    && !inSingleQuotes
                    && !inDoubleQuotes) {

                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts;
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            String input = scanner.nextLine();

            List<String> tokens = parseCommand(input);

            if (tokens.isEmpty()) {
                continue;
            }

            String command = tokens.get(0);

            if (command.equals("exit")) {
                break;

            } else if (command.equals("pwd")) {
                System.out.println(currentDirectory.getCanonicalPath());

            } else if (command.equals("cd")) {
                if (tokens.size() < 2) {
                    continue;
                }

                String path = tokens.get(1);

                File dir;

                if (path.equals("~")) {
                    dir = new File(System.getenv("HOME"));
                } else if (path.startsWith("/")) {
                    dir = new File(path);
                } else {
                    dir = new File(currentDirectory, path);
                }

                if (dir.exists() && dir.isDirectory()) {
                    currentDirectory = dir.getCanonicalFile();
                } else {
                    System.out.println("cd: " + path + ": No such file or directory");
                }

            } else if (command.equals("echo")) {
                StringBuilder output = new StringBuilder();

                for (int i = 1; i < tokens.size(); i++) {
                    if (i > 1) {
                        output.append(" ");
                    }
                    output.append(tokens.get(i));
                }

                System.out.println(output);

            } else if (command.equals("type")) {
                if (tokens.size() < 2) {
                    continue;
                }

                String target = tokens.get(1);

                if (target.equals("echo")
                        || target.equals("exit")
                        || target.equals("type")
                        || target.equals("pwd")
                        || target.equals("cd")) {

                    System.out.println(target + " is a shell builtin");

                } else {
                    String pathEnv = System.getenv("PATH");
                    String[] paths = pathEnv.split(File.pathSeparator);

                    boolean found = false;

                    for (String path : paths) {
                        File file = new File(path, target);

                        if (file.exists() && file.canExecute()) {
                            System.out.println(target + " is " + file.getAbsolutePath());
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        System.out.println(target + ": not found");
                    }
                }

            } else {
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
                    ProcessBuilder pb = new ProcessBuilder(tokens);
                    pb.directory(currentDirectory);
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