output codes
0	ExitCode.ok	Everything worked as expected.
1	ExitCode.bad_args	Invalid arguments, exited with an error.
2	ExitCode.input_file	The input file does not seem to be a valid PDF.
3	ExitCode.missing_dependency	An external program required by OCRmyPDF is missing.
4	ExitCode.invalid_output_pdf	An output file was created, but it does not seem to be a valid PDF. The file will be available.
5	ExitCode.file_access_error	The user running OCRmyPDF does not have sufficient permissions to read the input file and write the output file.
6	ExitCode.already_done_ocr	The file already appears to contain text so it may not need OCR. See output message.
7	ExitCode.child_process_error	An error occurred in an external program (child process) and OCRmyPDF cannot continue.
8	ExitCode.encrypted_pdf	The input PDF is encrypted. OCRmyPDF does not read encrypted PDFs. Use another program such as qpdf to remove encryption.
9	ExitCode.invalid_config	A custom configuration file was forwarded to Tesseract using --tesseract-config, and Tesseract rejected this file.
10	ExitCode.pdfa_conversion_failed	A valid PDF was created, PDF/A conversion failed. The file will be available.
15	ExitCode.other_error	Some other error occurred.
130	ExitCode.ctrl_c	The program was interrupted by pressing Ctrl+C.
