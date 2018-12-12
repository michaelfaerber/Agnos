package alu.linking.preprocessing.fileparser;

import alu.linking.preprocessing.fileparser.input.CSVParserGenericWrapper;
import alu.linking.preprocessing.fileparser.input.FileInParser;
import alu.linking.preprocessing.fileparser.input.NxParserGenericWrapper;
import alu.linking.preprocessing.fileparser.output.CSVOutputParser;
import alu.linking.preprocessing.fileparser.output.NxOutputParser;
import alu.linking.preprocessing.fileparser.output.OutParser;

public enum EnumFileType {
	N3(NxParserGenericWrapper.class, NxOutputParser.class), //
	CSV(CSVParserGenericWrapper.class, CSVOutputParser.class) //
	;
	public final Class<? extends FileInParser> parserInClass;
	public final Class<? extends OutParser> parserOutClass;

	private EnumFileType(Class<? extends FileInParser> fileInParserClass,
			Class<? extends OutParser> fileOutParserClass) {
		this.parserInClass = fileInParserClass;
		this.parserOutClass = fileOutParserClass;

	}
}
