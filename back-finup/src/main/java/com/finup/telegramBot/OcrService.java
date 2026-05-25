package com.finup.telegramBot;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class OcrService {

    private final Tesseract tesseract;

    public OcrService() {
        tesseract = new Tesseract();
        tesseract.setDatapath("tessdata");
        tesseract.setLanguage("por");
    }

    public String extrairTexto(File imagem) {
        try {
            BufferedImage bufferedImage = ImageIO.read(imagem);

            if (bufferedImage == null) {
                System.out.println("ImageIO não conseguiu ler a imagem: " + imagem.getName());
                return "";
            }

            return tesseract.doOCR(bufferedImage);

        } catch (TesseractException | IOException e) {
            System.out.println("Erro no OCR: " + e.getMessage());
            return "";
        }
    }
}