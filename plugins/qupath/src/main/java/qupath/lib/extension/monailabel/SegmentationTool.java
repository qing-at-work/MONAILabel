package qupath.lib.extension.monailabel;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.input.MouseEvent;
import qupath.lib.extension.monailabel.MonaiLabelClient.ResponseInfo;
import qupath.lib.extension.monailabel.commands.RunInference;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.viewer.tools.RectangleTool;
import qupath.lib.objects.PathObject;
import qupath.lib.plugins.parameters.ParameterList;
import qupath.lib.roi.RectangleROI;
import qupath.lib.roi.interfaces.ROI;

public class SegmentationTool extends RectangleTool {
	private final static Logger logger = LoggerFactory.getLogger(SegmentationTool.class);

	private static String selectedModel;
	private static int selectedTileSize = 1024;
	private static ResponseInfo info;

	public void mouseReleased(MouseEvent e) {
		var viewer = getViewer();
		if (viewer == null || viewer.getImageData() == null) {
			return;
		}
		logger.info("+++++++ Segmentation Tool... new ROI created...");
		super.mouseReleased(e);

		PathObject selectedObject = viewer.getSelectedObject();
		if (selectedObject == null)
			return;

		ROI roi = selectedObject.getROI();
		if (roi == null || !(roi instanceof RectangleROI) || roi.isEmpty())
			return;

		try {
			if (info == null) {
				info = MonaiLabelClient.info();
				List<String> names = new ArrayList<String>();
				for (String n : info.models.keySet()) {
					logger.info("Model: " + n + "; Type: " + info.models.get(n).type);
					if (info.models.get(n).type.equalsIgnoreCase("segmentation")) {
						names.add(n);
					}
				}
				int tileSize = selectedTileSize;
				if (names.size() == 0) {
					return;
				}
				if (names.size() == 1) {
					selectedModel = names.get(0);
				}

				if (selectedModel == null || selectedModel.isEmpty()) {
					ParameterList list = new ParameterList();
					list.addChoiceParameter("Model", "Model Name", names.get(0), names);
					list.addIntParameter("TileSize", "TileSize", tileSize);

					if (!Dialogs.showParameterDialog("MONAILabel", list)) {
						return;
					}

					selectedModel = (String) list.getChoiceParameterValue("Model");
					selectedTileSize = list.getIntParameterValue("TileSize").intValue();
				}
			}

			if (selectedModel.isBlank()) {
				return;
			}

			int[] bbox = Utils.getBBOX(roi);
			RunInference.runInference(selectedModel, info, bbox, selectedTileSize, viewer.getImageData());
		} catch (Exception ex) {
			ex.printStackTrace();
			Dialogs.showErrorMessage("MONAILabel", ex);
		}
	}
}
