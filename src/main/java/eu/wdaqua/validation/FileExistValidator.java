package eu.wdaqua.validation;

import java.io.File;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class FileExistValidator implements IParameterValidator {
    public void validate(String name, String value) throws ParameterException {
        File file =  new File(value);
        if(!file.exists() || file.isDirectory()) {
            throw new ParameterException("Parameter " + name + " which should represent a file that exists, does not exist.");
        }
    }
}
