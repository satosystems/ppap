package io.github.satosystems.ppap;

import io.vavr.control.Either;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Pattern;

public final class Main {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_GREEN = "\u001B[32m";

    @NotNull
    private static Either<String, String> resolveArchiveFileName(@NotNull final CommandLine commandLine) throws IOException {
        final var args = commandLine.getArgs();
        if (args.length == 0) {
            return Either.left("Error: No files/dirs");
        }
        final var basename = args.length == 1 ? new File(args[0]).getName() : "archive";
        final var files = Files.list(Path.of("."))
                .map(path -> path.getFileName().toString())
                .filter(path -> path.startsWith(basename))
                .toList();
        var suffix = "";
        while (files.contains(basename + suffix + ".zip")) {
            suffix = suffix.equals("") ? ".1" : "." + (Integer.parseInt(suffix.substring(1)) + 1);
        }
        return Either.right(basename + suffix + ".zip");
    }

    @NotNull
    private static Either<String, Pair<List<File>, List<File>>> checkNotExists(@NotNull final CommandLine commandLine) {
        final var args = commandLine.getArgs();
        final var files = new ArrayList<File>();
        final var dirs = new ArrayList<File>();
        for (final var arg : args) {
            final var file = new File(arg);
            if (!file.exists()) {
                return Either.left(arg);
            }
            if (file.isFile()) {
                files.add(file);
            } else {
                dirs.add(file);
            }
        }
        return Either.right(Pair.of(files, dirs));
    }

    @NotNull
    private static String makeRandomPassword() {
        final var range = 0x7f - 0x21; // from '!' to '~'
        final var random = new Random();
        final var sb = new StringBuilder();
        for (var i = 0; i < 16; i++) {
            final var c = random.nextInt(range) + 0x21;
            sb.append((char) c);
        }
        return new String(sb);
    }

    @NotNull
    private static Either<String, String> readProperty(@NotNull String key, @NotNull String defaultValue) throws IOException {
        final var config = new File(System.getenv("HOME"), ".ppaprc");
        if (!config.isFile()) {
            return Either.left(defaultValue);
        }
        final var props = new Properties();
        props.load(Files.newBufferedReader(Paths.get(config.toURI())));
        final var value = props.getProperty(key);
        return value == null ? Either.left(defaultValue) : Either.right(value);
    }

    @NotNull
    private static Either<String, String> readPassword(final boolean needPassword) throws IOException {
        if (needPassword) {
            final var console = System.console();
            if (console == null) {
                System.err.println(ANSI_PURPLE + "Warning: No console available. You cannot input password." + ANSI_RESET);
            } else {
                while (true) {
                    final var password = new String(console.readPassword("Enter password: "));
                    final var passwordVerify = new String(console.readPassword("Verify password: "));
                    if (password.equals(passwordVerify)) {
                        return Either.right(password);
                    }
                    System.err.println(ANSI_RED + "Wrong password. " + ANSI_RESET);
                }
            }
        }
        return readProperty("password", makeRandomPassword());
    }

    @NotNull
    private static Pair<Optional<CommandLine>, Options> parseCommandLine(@NotNull final String... args) {
        final var options = new Options();
        options.addOption("p", "password", false, "Input password interactively.");
        options.addOption("h", "help", false, "Show this help.");
        options.addOption("v", "version", false, "Show version.");
        try {
            final var parser = new DefaultParser();
            return Pair.of(Optional.of(parser.parse(options, args)), options);
        } catch (final ParseException e) {
            return Pair.of(Optional.empty(), options);
        }
    }

    private static void showHelp(@NotNull final Options options) {
        final var helpFormatter = new HelpFormatter();
        final var header = "\nThis is a CLI program that creates a ZIP file with a password.\n";
        final var footer = "\nPlease report issues at https://github.com/satosystems/ppap/issues";
        helpFormatter.printHelp("ppap [-h] [-p] [-v] [FILES/DIRS]", header, options, footer);
    }

    @NotNull
    private static Path getApplicationPath() throws URISyntaxException {
        final var pd = Main.class.getProtectionDomain();
        final var cs = pd.getCodeSource();
        final var location = cs.getLocation();
        final URI uri = location.toURI();
        return Paths.get(uri);
    }

    @NotNull
    private static String getVersion() throws URISyntaxException {
        final var path = getApplicationPath();
        final var fileName = path.getFileName().toString();
        return fileName
                .replaceAll("ppap-", "")
                .replaceAll("\\.jar", "");
    }

    @NotNull
    private static String readCharset() throws IOException {
        final var charset = readProperty("charset", "windows-31j");
        return charset.isRight() ? charset.get() : charset.getLeft();
    }

    @NotNull
    private static Optional<Pattern> readIgnore() throws IOException {
        final var ignore = readProperty("ignore", "");
        return ignore.isRight() ? Optional.of(Pattern.compile(ignore.get())) : Optional.empty();
    }

    private static void addFilesRecursive(@NotNull ZipFile zipFile, @NotNull ZipParameters params, @Nullable Pattern ignore, @NotNull Pair<List<File>, List<File>> pairs) throws IOException {
        if (!pairs.getLeft().isEmpty()) {
            final var files = pairs.getLeft();
            final var filtered = ignore == null ? files : files.stream().filter(file -> !ignore.matcher(file.getName()).matches()).toList();
            zipFile.addFiles(filtered, params);
        }
        for (final var dir : pairs.getRight()) {
            if (ignore != null && ignore.matcher(dir.getName()).matches()) {
                continue;
            }
            final var pairsOfDir = Files.list(Path.of(dir.toURI()))
                    .map(Path::toFile)
                    .reduce(Pair.of((List<File>) new ArrayList<File>(), (List<File>) new ArrayList<File>()), (acc, cur) -> {
                final var list = cur.isFile() ? acc.getLeft() : acc.getRight();
                list.add(cur);
                return acc;
            }, (acc, cur) -> acc);
            addFilesRecursive(zipFile, params, ignore, pairsOfDir);
        }
    }

    public static void main(@NotNull final String... args) throws IOException, URISyntaxException {
        final var commandLinePair = parseCommandLine(args);
        final var options = commandLinePair.getRight();
        if (commandLinePair.getLeft().isEmpty()) {
            showHelp(options);
            System.exit(-1);
        }
        final var commandLine = commandLinePair.getLeft().get();
        if (commandLine.hasOption("h")) {
            showHelp(options);
            System.exit(0);
        }
        if (commandLine.hasOption("v")) {
            System.out.println(getVersion());
            System.exit(0);
        }

        final var archiveFileNameOrError = resolveArchiveFileName(commandLine);
        if (archiveFileNameOrError.isLeft()) {
            System.out.println(ANSI_RED + archiveFileNameOrError.getLeft() + ANSI_RESET);
            System.exit(-1);
        }
        final var archiveFileName = archiveFileNameOrError.get();

        final var notExistsOrFiles = checkNotExists(commandLine);
        if (notExistsOrFiles.isLeft()) {
            System.out.println(ANSI_RED + "Error: Not found \"" + notExistsOrFiles.getLeft() + "\"" + ANSI_RESET);
            System.exit(-1);
        }
        final var pairs = notExistsOrFiles.get();

        final var params = new ZipParameters();
        params.setEncryptFiles(true);
        params.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

        final var eitherPassword = readPassword(commandLine.hasOption("p"));
        final var password = eitherPassword.isLeft() ? eitherPassword.getLeft() : eitherPassword.get();
        final var ignore = readIgnore();

        final var zipFile = new ZipFile(archiveFileName);
        zipFile.setCharset(Charset.forName(readCharset()));
        zipFile.setPassword(password.toCharArray());
        addFilesRecursive(zipFile, params, ignore.isEmpty() ? null : ignore.get(),pairs);
        System.out.println("Created: " + ANSI_GREEN + archiveFileName + ANSI_RESET);
        if (eitherPassword.isLeft()) {
            System.out.println("Password is: " + ANSI_GREEN + password + ANSI_RESET);
        }
    }
}
