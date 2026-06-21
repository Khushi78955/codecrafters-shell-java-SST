import java.util.Scanner;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static File currentDirectory = new File(System.getProperty("user.dir"));
    private static int nextJobNumber = 1;

    private static class Job {
        int jobNumber;
        long pid;
        String command;
        Process process;

        Job(int jobNumber, long pid, String command, Process process) {
            this.jobNumber = jobNumber;
            this.pid = pid;
            this.command = command;
            this.process = process;
        }
    }

    private static List<Job> jobs = new ArrayList<>();

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
            boolean backgroundJob = false;

            if (!tokens.isEmpty() && tokens.get(tokens.size() - 1).equals("&")) {
                backgroundJob = true;
                tokens.remove(tokens.size() - 1);
            }

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

            } else if (command.equals("jobs")) {

                List<Job> jobsToRemove = new ArrayList<>();

                int lastIndex = jobs.size() - 1;
                int secondLastIndex = jobs.size() - 2;

                for (int i = 0; i < jobs.size(); i++) {

                    Job job = jobs.get(i);

                    char marker = ' ';

                    if (i == lastIndex) {
                        marker = '+';
                    } else if (i == secondLastIndex) {
                        marker = '-';
                    }

                    if (job.process.isAlive()) {

                        System.out.printf(
                                "[%d]%c  %-24s%s%n",
                                job.jobNumber,
                                marker,
                                "Running",
                                job.command);

                    } else {

                        String doneCommand = job.command;

                        if (doneCommand.endsWith(" &")) {
                            doneCommand = doneCommand.substring(
                                    0,
                                    doneCommand.length() - 2);
                        }

                        System.out.printf(
                                "[%d]%c  %-24s%s%n",
                                job.jobNumber,
                                marker,
                                "Done",
                                doneCommand);

                        jobsToRemove.add(job);
                    }
                }

                jobs.removeAll(jobsToRemove);

            } else if (command.equals("echo")) {

                int stdoutRedirect = -1;
                int stderrRedirect = -1;

                for (int i = 0; i < tokens.size(); i++) {

                    if (tokens.get(i).equals(">")
                            || tokens.get(i).equals("1>")
                            || tokens.get(i).equals(">>")
                            || tokens.get(i).equals("1>>")) {
                        stdoutRedirect = i;
                    }

                    if (tokens.get(i).equals("2>")
                            || tokens.get(i).equals("2>>")) {
                        stderrRedirect = i;
                    }
                }

                StringBuilder output = new StringBuilder();

                int end = tokens.size();

                if (stdoutRedirect != -1) {

                    end = Math.min(end, stdoutRedirect);

                }

                if (stderrRedirect != -1) {

                    end = Math.min(end, stderrRedirect);

                }

                for (int i = 1; i < end; i++) {
                    if (i > 1) {
                        output.append(" ");
                    }
                    output.append(tokens.get(i));
                }

                if (stdoutRedirect != -1) {

                    String op = tokens.get(stdoutRedirect);

                    if (op.equals(">>") || op.equals("1>>")) {

                        java.nio.file.Files.writeString(
                                java.nio.file.Path.of(tokens.get(stdoutRedirect + 1)),
                                output.toString() + System.lineSeparator(),
                                java.nio.file.StandardOpenOption.CREATE,
                                java.nio.file.StandardOpenOption.APPEND);

                    } else {

                        java.nio.file.Files.writeString(
                                java.nio.file.Path.of(tokens.get(stdoutRedirect + 1)),
                                output.toString() + System.lineSeparator());
                    }

                } else {

                    System.out.println(output);
                }

                if (stderrRedirect != -1) {

                    String op = tokens.get(stderrRedirect);

                    if (op.equals("2>>")) {

                        java.nio.file.Files.writeString(
                                java.nio.file.Path.of(tokens.get(stderrRedirect + 1)),
                                "",
                                java.nio.file.StandardOpenOption.CREATE,
                                java.nio.file.StandardOpenOption.APPEND);

                    } else {

                        java.nio.file.Files.writeString(
                                java.nio.file.Path.of(tokens.get(stderrRedirect + 1)),
                                "");
                    }
                }

            } else if (command.equals("type")) {
                if (tokens.size() < 2) {
                    continue;
                }

                String target = tokens.get(1);

                if (target.equals("echo")
                        || target.equals("exit")
                        || target.equals("type")
                        || target.equals("pwd")
                        || target.equals("cd")
                        || target.equals("jobs")) {

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
                    int stdoutRedirect = -1;
                    int stderrRedirect = -1;
                    for (int i = 0; i < tokens.size(); i++) {

                        if (tokens.get(i).equals(">")
                                || tokens.get(i).equals("1>")
                                || tokens.get(i).equals(">>")
                                || tokens.get(i).equals("1>>")) {
                            stdoutRedirect = i;
                        }

                        if (tokens.get(i).equals("2>")
                                || tokens.get(i).equals("2>>")) {
                            stderrRedirect = i;
                        }
                    }

                    int cutIndex = tokens.size();

                    if (stdoutRedirect != -1) {
                        cutIndex = Math.min(cutIndex, stdoutRedirect);
                    }

                    if (stderrRedirect != -1) {
                        cutIndex = Math.min(cutIndex, stderrRedirect);
                    }

                    List<String> commandTokens = new ArrayList<>(tokens.subList(0, cutIndex));
                    ProcessBuilder pb = new ProcessBuilder(commandTokens);

                    if (stdoutRedirect != -1) {

                        String op = tokens.get(stdoutRedirect);

                        if (op.equals(">>") || op.equals("1>>")) {

                            pb.redirectOutput(
                                    ProcessBuilder.Redirect.appendTo(
                                            new File(tokens.get(stdoutRedirect + 1))));

                        } else {

                            pb.redirectOutput(
                                    ProcessBuilder.Redirect.to(
                                            new File(tokens.get(stdoutRedirect + 1))));
                        }

                    } else {
                        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    }

                    if (stderrRedirect != -1) {

                        String op = tokens.get(stderrRedirect);

                        if (op.equals("2>>")) {

                            pb.redirectError(
                                    ProcessBuilder.Redirect.appendTo(
                                            new File(tokens.get(stderrRedirect + 1))));

                        } else {

                            pb.redirectError(
                                    ProcessBuilder.Redirect.to(
                                            new File(tokens.get(stderrRedirect + 1))));
                        }

                    } else {

                        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                    }

                    pb.directory(currentDirectory);
                    if (backgroundJob) {

                        pb.inheritIO();

                    }

                    Process process = pb.start();

                    if (backgroundJob) {

                        System.out.println("[" + nextJobNumber + "] " + process.pid());

                        jobs.add(
                                new Job(
                                        nextJobNumber,
                                        process.pid(),
                                        input,
                                        process));

                        nextJobNumber++;

                    } else {

                        process.waitFor();
                    }
                }

                else {
                    System.out.println(input + ": command not found");
                }
            }
        }
    }
}