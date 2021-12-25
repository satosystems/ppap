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

public final class Main {
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
    private static Either<String, String> readPassword(final boolean needPassword) throws IOException {
        if (needPassword) {
            final var console = System.console();
            if (console == null) {
                System.err.println("Warning: No console available. You cannot input password.");
            } else {
                while (true) {
                    final var password = new String(console.readPassword("Enter password: "));
                    final var passwordVerify = new String(console.readPassword("Verify password: "));
                    if (password.equals(passwordVerify)) {
                        return Either.right(password);
                    }
                    System.err.print("Wrong password. ");
                }
            }
        }
        final var config = new File(System.getenv("HOME"), ".ppaprc");
        if (!config.isFile()) {
            return Either.left(makeRandomPassword());
        }
        final var props = new Properties();
        props.load(Files.newBufferedReader(Paths.get(config.toURI())));
        return Either.right(props.getProperty("password"));
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
            System.err.println(archiveFileNameOrError.getLeft());
            System.exit(-1);
        }
        final var archiveFileName = archiveFileNameOrError.get();

        final var notExistsOrFiles = checkNotExists(commandLine);
        if (notExistsOrFiles.isLeft()) {
            System.err.println("Error: Not found " + notExistsOrFiles.getLeft());
            System.exit(-1);
        }
        final var pairs = notExistsOrFiles.get();

        final var params = new ZipParameters();
        params.setEncryptFiles(true);
        params.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

        final var eitherPassword = readPassword(commandLine.hasOption("p"));
        final var password = eitherPassword.isLeft() ? eitherPassword.getLeft() : eitherPassword.get();

        final var zipFile = new ZipFile(archiveFileName);
        zipFile.setCharset(Charset.forName("windows-31j"));
        zipFile.setPassword(password.toCharArray());
        if (!pairs.getLeft().isEmpty()) {
            zipFile.addFiles(pairs.getLeft(), params);
        }
        for (final var dir : pairs.getRight()) {
            zipFile.addFolder(dir, params);
        }
        System.out.println("Created: " + archiveFileName);
        if (eitherPassword.isLeft()) {
            System.out.println("Password is: " + password);
        }
    }
}
