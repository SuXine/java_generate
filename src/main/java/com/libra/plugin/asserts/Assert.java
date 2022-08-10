
package com.libra.plugin.asserts;


import com.intellij.openapi.ui.Messages;
import com.libra.plugin.exception.GenerateMarkDownException;


public class Assert {

    public static void isNotNull(Object o, String message) {

        if (null == o) {

            Messages.showErrorDialog(message, "生成错误！");

            throw new GenerateMarkDownException(message);

        }

    }

}


