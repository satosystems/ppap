package io.github.satosystems.ppap;

import io.vavr.control.Either;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public final class Main {
    @NotNull
    private static Either<String, String> resolveArchiveFileName(@NotNull final String... args) throws IOException {
        if (args.length == 0) {
            return Either.left("Error: no parameters");
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
    private static Either<String, Pair<List<File>, List<File>>> checkNotExists(@NotNull final String... args) {
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
    private static Either<String, String> readPassword() throws IOException {
        final var config = new File(System.getenv("HOME"), ".ppaprc");
        if (!config.isFile()) {
            return Either.left(makeRandomPassword());
        }
        final var props = new Properties();
        props.load(Files.newBufferedReader(Paths.get(config.toURI())));
        return Either.right(props.getProperty("password"));
    }

    public static void main(@NotNull final String... args) throws IOException {
        final var archiveFileNameOrError = resolveArchiveFileName(args);
        if (archiveFileNameOrError.isLeft()) {
            System.err.println(archiveFileNameOrError.getLeft());
            System.exit(-1);
        }
        final var archiveFileName = archiveFileNameOrError.get();

        final var notExistsOrFiles = checkNotExists(args);
        if (notExistsOrFiles.isLeft()) {
            System.err.println("Error: not found: " + notExistsOrFiles.getLeft());
            System.exit(-1);
        }
        final var pairs = notExistsOrFiles.get();

        final var params = new ZipParameters();
        params.setEncryptFiles(true);
        params.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

        final var eitherPassword = readPassword();
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
