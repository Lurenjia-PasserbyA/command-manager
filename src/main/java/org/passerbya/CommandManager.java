package org.passerbya;

import java.io.*;
import java.lang.ProcessBuilder;

public class CommandManager {

    public static void main(String[] args) {
        try {
            ProcessBuilder pd = new ProcessBuilder("powershell.exe");
            Process process = pd.start();

            Reader OutputReader = new InputStreamReader(process.getInputStream());
            BufferedReader stdout = new BufferedReader(OutputReader);

            OutputStream stdin = process.getOutputStream();

            Reader ErrorReader = new InputStreamReader(process.getErrorStream());
            BufferedReader stderr =new BufferedReader(ErrorReader);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}