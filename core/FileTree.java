package core;


import java.io.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

public class FileTree {
    private final String directory;
     private String fileTree = "";

    FileTree(String directory){
        this.directory = directory;
    }

    public void map() throws IOException {
        try(Stream<Path> paths = Files.walk(Paths.get(directory))){
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String capped = path.toString().substring(directory.length() + 1); //capping the root directory out
                        fileTree += capped;
                        fileTree += ":";

                        try {
                            MessageDigest md = MessageDigest.getInstance("MD5");
                            fileTree += checksum(md, new File(path.toString()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        fileTree += "#";
                    });
        }
    }

    private String checksum(MessageDigest digest, File file) throws IOException{
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] chunk = new byte[1024];
        int byteCount = 0;

        while((byteCount = fileInputStream.read(chunk)) != -1){
            digest.update(chunk, 0, byteCount);
        }

        fileInputStream.close();

        byte[] hash = digest.digest();

        StringBuilder sBuilder = new StringBuilder();
        for (byte b : hash) {
            sBuilder.append(Integer
                    .toString((b & 0xFF) + 0x100, 16)
                    .substring(1));
        }
        return sBuilder.toString();
    }

    public void safe(String output) throws IOException{
        FileWriter fWriter = new FileWriter(output);
        fWriter.write(fileTree);
        fWriter.close();
    }

    public String get(){
        return fileTree;
    }



}
