package com.adobe.sign.adobelibrary.utils;

import java.io.*;

public class FileUtils {

	public static void writeToFile(Object classType, String fileName) throws IOException {
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(fileName));
		objectOutputStream.writeObject(classType);
	}

	public static Object readFile(String fileName)throws IOException, ClassNotFoundException {
		ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(fileName));
        return objectInputStream.readObject();
	}
}