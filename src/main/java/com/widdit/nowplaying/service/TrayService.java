package com.widdit.nowplaying.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.net.URI;

@Slf4j
@Service
@ConditionalOnProperty(name = "now-playing.embedded", havingValue = "false", matchIfMissing = true)
public class TrayService {

    private static final String HOME_URL = "http://127.0.0.1:9863/";

    private final ConfigurableApplicationContext applicationContext;
    private volatile TrayIcon trayIcon;

    public TrayService(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void initialize() {
        EventQueue.invokeLater(this::createTrayIcon);
    }

    private void createTrayIcon() {
        if (!SystemTray.isSupported()) {
            log.warn("当前系统不支持系统托盘，Now Playing 将继续在后台运行");
            return;
        }

        try {
            PopupMenu menu = new PopupMenu();
            MenuItem openItem = new MenuItem("打开 Now Playing");
            openItem.addActionListener(event -> openHomePage());
            menu.add(openItem);
            menu.addSeparator();

            MenuItem exitItem = new MenuItem("退出 Now Playing");
            exitItem.addActionListener(event -> exitApplication());
            menu.add(exitItem);

            TrayIcon icon = new TrayIcon(createIconImage(), "Now Playing Lilith", menu);
            icon.setImageAutoSize(true);
            icon.addActionListener(event -> openHomePage());
            SystemTray.getSystemTray().add(icon);
            trayIcon = icon;
            log.info("Now Playing 系统托盘已启动");
        } catch (Exception e) {
            log.warn("无法创建 Now Playing 系统托盘，应用将继续在后台运行", e);
        }
    }

    private Image createIconImage() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(31, 25, 45));
            graphics.fillOval(1, 1, 30, 30);
            graphics.setColor(new Color(167, 139, 250));
            graphics.fillOval(5, 5, 22, 22);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            graphics.drawString("NP", 7, 21);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private void openHomePage() {
        try {
            URI uri = URI.create(HOME_URL);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else {
                new ProcessBuilder("explorer.exe", HOME_URL).start();
            }
        } catch (Exception e) {
            log.warn("无法打开 Now Playing 页面: {}", HOME_URL, e);
        }
    }

    private void exitApplication() {
        Thread shutdownThread = new Thread(() -> {
            int exitCode = SpringApplication.exit(applicationContext);
            System.exit(exitCode);
        }, "now-playing-shutdown");
        shutdownThread.setDaemon(false);
        shutdownThread.start();
    }

    @PreDestroy
    public void removeTrayIcon() {
        TrayIcon icon = trayIcon;
        if (icon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(icon);
            trayIcon = null;
        }
    }
}
