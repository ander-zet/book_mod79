package com.example.customdiary.client;

import com.example.customdiary.config.DiaryConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class DiaryScreen extends Screen {
    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    private static final ResourceLocation PAGE_FORWARD_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_forward");
    private static final ResourceLocation PAGE_FORWARD_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_forward_highlighted");
    private static final ResourceLocation PAGE_BACKWARD_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_backward");
    private static final ResourceLocation PAGE_BACKWARD_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_backward_highlighted");

    private final int currentChapterId;
    private final DiaryConfig config;
    private final List<DiaryConfig.Page> pagesToRender = new ArrayList<>();
    private int currentPage = 0;

    // Оригинальные размеры ванильной книги Minecraft
    private final int imageWidth = 192;
    private final int imageHeight = 192;

    public DiaryScreen(int currentChapterId, DiaryConfig config) {
        super(Component.literal("Diary"));
        this.currentChapterId = currentChapterId;
        this.config = config;
        buildPageList();
    }

    private void buildPageList() {
        if (config == null || config.chapters == null) return;
        for (DiaryConfig.Chapter chapter : config.chapters) {
            if (chapter.id <= currentChapterId && chapter.pages != null) {
                pagesToRender.addAll(chapter.pages);
            }
        }
    }

    @Override
    protected void init() {
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;

        // Позиции стрелок перелистывания для 192x192
        this.addRenderableWidget(new PageArrowButton(left + 38, top + 156, false, btn -> {
            if (currentPage > 0) currentPage--;
        }));

        this.addRenderableWidget(new PageArrowButton(left + 120, top + 156, true, btn -> {
            if (currentPage < pagesToRender.size() - 1) currentPage++;
        }));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Отключение шейдерного размытия мира
        this.renderMenuBackground(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;

        // Отрисовка оригинальной полноразмерной книги
        guiGraphics.blit(BOOK_TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        if (!pagesToRender.isEmpty() && currentPage < pagesToRender.size()) {
            DiaryConfig.Page page = pagesToRender.get(currentPage);

            String titleStr = (currentPage + 1) + " / " + pagesToRender.size();
            int titleWidth = this.font.width(titleStr);
            int textX = left + 36;
            int printableAreaWidth = 114;

            // Номер страницы
            guiGraphics.drawString(this.font, titleStr, textX + (printableAreaWidth - titleWidth) / 2, top + 18, 0x000000, false);

            // Текст страницы
            guiGraphics.drawWordWrap(this.font, Component.literal(page.text), textX, top + 30, printableAreaWidth, 0x000000);

            // Изображение страницы
            if (page.image != null && !page.image.isEmpty()) {
                try {
                    ResourceLocation imgLoc = ResourceLocation.parse(page.image);
                    guiGraphics.blit(imgLoc, textX + (printableAreaWidth - 48) / 2, top + 90, 0, 0, 48, 48, 48, 48);
                } catch (Exception ignored) {}
            }
        } else {
            guiGraphics.drawString(this.font, "Дневник пуст...", left + 36, top + 30, 0x000000, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class PageArrowButton extends Button {
        private final boolean isForward;

        public PageArrowButton(int x, int y, boolean isForward, OnPress onPress) {
            super(x, y, 23, 13, Component.empty(), onPress, DEFAULT_NARRATION);
            this.isForward = isForward;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation sprite = this.isForward
                    ? (this.isHoveredOrFocused() ? PAGE_FORWARD_HIGHLIGHTED_SPRITE : PAGE_FORWARD_SPRITE)
                    : (this.isHoveredOrFocused() ? PAGE_BACKWARD_HIGHLIGHTED_SPRITE : PAGE_BACKWARD_SPRITE);
            guiGraphics.blitSprite(sprite, this.getX(), this.getY(), this.width, this.height);
        }
    }
}