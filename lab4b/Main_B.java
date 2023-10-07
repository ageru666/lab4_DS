package lab4b;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Garden {
    private int[][] garden;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Garden(int rows, int cols) {
        garden = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                garden[i][j] = 1;
            }
        }
    }

    public void waterPlants() {
        lock.writeLock().lock();
        try {
            Random rand = new Random();
            int row = rand.nextInt(garden.length);
            int col = rand.nextInt(garden[row].length);
            if (garden[row][col] == 0) {
                garden[row][col] = 1;
                System.out.println("Садівник полив рослину на рядку " + row + " та колонці " + col);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void changeNature() {
        lock.writeLock().lock();
        try {
            Random rand = new Random();
            int row = rand.nextInt(garden.length);
            int col = rand.nextInt(garden[row].length);
            int newState = rand.nextInt(2);
            garden[row][col] = newState;
            System.out.println("Природа змінила стан рослини на рядку " + row + " та колонці " + col + " на " + newState);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void printGardenToFile(String filename) {
        lock.readLock().lock();
        try {
            FileWriter writer = new FileWriter(filename, true);
            for (int i = 0; i < garden.length; i++) {
                for (int j = 0; j < garden[i].length; j++) {
                    writer.write(garden[i][j] + " ");
                }
                writer.write("\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void printGardenToScreen(OutputStream output) {
        lock.readLock().lock();
        PrintStream printStream = new PrintStream(output);
        try {
            for (int i = 0; i < garden.length; i++) {
                for (int j = 0; j < garden[i].length; j++) {
                    printStream.print(garden[i][j] + " ");
                }
                printStream.println();
            }
        } finally {
            lock.readLock().unlock();
        }
    }
}

public class Main_B {
    public static void main(String[] args) {
        Garden garden = new Garden(5, 5);

        Thread gardenerThread = new Thread(() -> {
            while (true) {
                garden.waterPlants();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread natureThread = new Thread(() -> {
            while (true) {
                garden.changeNature();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread monitor1Thread = new Thread(() -> {
            while (true) {
                garden.printGardenToFile("garden.txt");
                try {
                    Thread.sleep(7000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread monitor2Thread = new Thread(() -> {
            while (true) {
                garden.printGardenToScreen(System.out);
                try {
                    Thread.sleep(7000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        gardenerThread.start();
        natureThread.start();
        monitor1Thread.start();
        monitor2Thread.start();

        try {
            gardenerThread.join();
            natureThread.join();
            monitor1Thread.join();
            monitor2Thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
