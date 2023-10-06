package lab4a;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.*;

class CustomReadWriteLock {
    private final ReentrantLock readLock = new ReentrantLock();
    private final ReentrantLock writeLock = new ReentrantLock();
    private int readers = 0;

    public void readLock() {
        readLock.lock();
        synchronized (this) {
            readers++;
        }
        readLock.unlock();
    }

    public void readUnlock() {
        synchronized (this) {
            readers--;
            if (readers == 0) {
                notify();
            }
        }
    }

    public void writeLock() {
        synchronized (this) {
            while (readers > 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        writeLock.lock();
    }

    public void writeUnlock() {
        writeLock.unlock();
    }
}

class Database {
    private Map<String, String> data = new HashMap<>();
    private final CustomReadWriteLock lock = new CustomReadWriteLock();
    private final File databaseFile = new File("database.txt");

    public String findPhoneNumber(String lastName) {
        lock.readLock();
        try {
            return data.get(lastName);
        } finally {
            lock.readUnlock();
        }
    }

    public String findNameByPhoneNumber(String phoneNumber) {
        lock.readLock();
        try {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (entry.getValue().equals(phoneNumber)) {
                    return entry.getKey();
                }
            }
            return null;
        } finally {
            lock.readUnlock();
        }
    }

    public void addOrUpdateEntry(String name, String phoneNumber) {
        lock.writeLock();
        try {
            data.put(name, phoneNumber);
            writeToFile(name + " - " + phoneNumber);
        } finally {
            lock.writeUnlock();
        }
    }

    public void deleteEntry(String name) {
        lock.writeLock();
        try {
            data.remove(name);
            removeFromFile(name);
        } finally {
            lock.writeUnlock();
        }
    }

    private void writeToFile(String entry) {
        try (FileWriter writer = new FileWriter(databaseFile, true);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            bufferedWriter.write(entry);
            bufferedWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeFromFile(String name) {
        try {
            lock.writeLock();
            Path filePath = databaseFile.toPath();
            List<String> lines = Files.readAllLines(filePath);
            List<String> updatedLines = new ArrayList<>();

            for (String line : lines) {
                if (!line.startsWith(name)) {
                    updatedLines.add(line);
                }
            }

            Files.write(filePath, updatedLines);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.writeUnlock();
        }
    }
}

class ReaderThread extends Thread {
    private final Database database;
    private final String lastName;

    public ReaderThread(Database database, String lastName) {
        this.database = database;
        this.lastName = lastName;
    }

    @Override
    public void run() {
        String phoneNumber = database.findPhoneNumber(lastName);
        if (phoneNumber != null) {
            System.out.println("Phone number for " + lastName + ": " + phoneNumber);
        } else {
            System.out.println("No phone number found for " + lastName);
        }
    }
}

class WriterThread extends Thread {
    private final Database database;
    private final String name;
    private final String phoneNumber;

    public WriterThread(Database database, String name, String phoneNumber) {
        this.database = database;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void run() {
        database.addOrUpdateEntry(name, phoneNumber);
        System.out.println("Added/Updated entry: " + name + " - " + phoneNumber);
    }
}

class PhoneNumberFinderThread extends Thread {
    private final Database database;
    private final String phoneNumber;

    public PhoneNumberFinderThread(Database database, String phoneNumber) {
        this.database = database;
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void run() {
        String name = database.findNameByPhoneNumber(phoneNumber);
        if (name != null) {
            System.out.println("Name for phone number " + phoneNumber + ": " + name);
        } else {
            System.out.println("No name found for phone number " + phoneNumber);
        }
    }
}

class NameToPhoneNumberFinderThread extends Thread {
    private final Database database;
    private final String name;

    public NameToPhoneNumberFinderThread(Database database, String name) {
        this.database = database;
        this.name = name;
    }

    @Override
    public void run() {
        String phoneNumber = database.findPhoneNumber(name);
        if (phoneNumber != null) {
            System.out.println("Find for phone number for " + name + ": " + phoneNumber);
        } else {
            System.out.println("No phone number found for " + name);
        }
    }
}

public class Main_A {
    public static void main(String[] args) {
        Database database = new Database();

       database.addOrUpdateEntry("Smith", "555-1111");
       database.addOrUpdateEntry("Johnson", "555-2222");


        ReaderThread reader1 = new ReaderThread(database, "Smith");
        ReaderThread reader2 = new ReaderThread(database, "Johnson");

        WriterThread writer1 = new WriterThread(database, "Jean", "555-1234");
        WriterThread writer2 = new WriterThread(database, "Brown", "555-5678");

        PhoneNumberFinderThread finder1 = new PhoneNumberFinderThread(database, "555-1234");

        NameToPhoneNumberFinderThread finder2 = new NameToPhoneNumberFinderThread(database, "Smith");

        reader1.start();
        reader2.start();
        writer1.start();
        writer2.start();

        finder1.start();

        finder2.start();

        try {
            reader1.join();
            reader2.join();
            writer1.join();
            writer2.join();
            finder1.join();
            finder2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
