package me.scai.plato.android.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.io.File;

import me.scai.plato.android.R;

public class FileSystemHelper {
    /* Ensure the existence of an sub-directory in the external file-system directory. Create the
     * sub-directory if necessary.
     */
    public static String ensureExternalDirectory(Context ctx, String subDirName) {
        File filesDir = ctx.getExternalFilesDir(null);

        if (filesDir == null) {
            AlertDialog errDialog = new AlertDialog.Builder(ctx).setMessage(R.string.externalFilesDirMissingErrorMsg)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            System.exit(1); /* TODO: Make it work */
                        }
                    }).create();

            errDialog.show();
        }

        String filesPath = filesDir.getPath();
        String externalDir = filesPath;

        String subDir = externalDir.endsWith("/") ? externalDir : externalDir + "/";
        subDir = subDir + subDirName;

        File dir = new File(subDir);
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        if (!dir.isDirectory()) {
            return null;
        } else {
            return subDir;
        }

    }

}
