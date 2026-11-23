package io.github.marvinwiget.mucke.app;

import io.github.marvinwiget.mucke.music.PlaybackPlan;
import io.github.marvinwiget.mucke.sound.LivePlayer;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class MixerPanel extends VBox {
    private final LivePlayer player;
    private final String[] names = new String[5];
    private final Label[] nameLabels = new Label[5];
    private final Label[] valueLabels = new Label[5];
    private final Slider[] sliders = new Slider[5];
    private final Slider sampleDelay;
    private final Label delayValue;
    private boolean updatingDelay;

    MixerPanel(LivePlayer player) {
        this.player = player;
        getStyleClass().add("mucke-sidebar");
        setMinWidth(268);
        setPrefWidth(268);
        setSpacing(12);
        setPadding(new Insets(16, 14, 12, 14));

        Label title = new Label("INSTRUMENTS");
        title.getStyleClass().add("mixer-heading");
        getChildren().add(title);

        for (int i = 0; i < 5; i++) {
            final int index = i;
            Label number = new Label(String.valueOf(i + 1));
            number.getStyleClass().add("mixer-slot");
            Label name = new Label("—");
            name.getStyleClass().add("mixer-name");
            Label value = new Label("—");
            value.getStyleClass().add("mixer-value");

            HBox header = new HBox(8, number, name, value);
            HBox.setHgrow(name, Priority.ALWAYS);
            name.setMaxWidth(Double.MAX_VALUE);

            Slider slider = new Slider(0, 1, 1);
            slider.setMaxWidth(Double.MAX_VALUE);
            slider.setDisable(true);
            slider.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (names[index] == null) return;
                player.setVolume(names[index], newValue.doubleValue());
                value.setText(Math.round(newValue.doubleValue() * 100) + "%");
            });

            VBox row = new VBox(4, header, slider);
            getChildren().add(row);
            nameLabels[i] = name;
            valueLabels[i] = value;
            sliders[i] = slider;
        }

        Label delayTitle = new Label("SAMPLE DELAY");
        delayTitle.getStyleClass().add("mixer-heading");
        delayValue = new Label();
        delayValue.getStyleClass().add("mixer-value");
        HBox delayHeader = new HBox(8, delayTitle, delayValue);
        HBox.setHgrow(delayTitle, Priority.ALWAYS);

        sampleDelay = new Slider(0, 200, 0);
        sampleDelay.setMaxWidth(Double.MAX_VALUE);
        sampleDelay.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingDelay) player.setSampleDelayMillis(newValue.doubleValue());
            delayValue.setText(Math.round(newValue.doubleValue()) + " ms");
        });
        getChildren().add(new VBox(4, delayHeader, sampleDelay));
    }

    void showPlan(PlaybackPlan plan) {
        updatingDelay = true;
        sampleDelay.setValue(player.getSampleDelayMillis());
        delayValue.setText(Math.round(sampleDelay.getValue()) + " ms");
        updatingDelay = false;
        for (int i = 0; i < names.length; i++) {
            String name = plan.slots.get(i + 1);
            names[i] = name;
            nameLabels[i].setText(name == null ? "—" : name);
            sliders[i].setDisable(name == null);
            if (name == null) {
                valueLabels[i].setText("—");
            } else {
                double volume = player.getVolume(name);
                sliders[i].setValue(volume);
                valueLabels[i].setText(Math.round(volume * 100) + "%");
            }
        }
    }
}
