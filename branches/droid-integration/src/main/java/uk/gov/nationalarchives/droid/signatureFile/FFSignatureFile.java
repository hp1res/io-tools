/*
 * c The National Archives 2005-2006.  All rights reserved.
 * See Licence.txt for full licence details.
 *
 * Developed by:
 * Tessella Support Services plc
 * 3 Vineyard Chambers
 * Abingdon, OX14 3PX
 * United Kingdom
 * http://www.tessella.com
 *
 * Tessella/NPD/4305
 * PRONOM 4
 *
 * $Id: FFSignatureFile.java,v 1.6 2006/03/13 15:15:29 linb Exp $
 *
 * $Log: FFSignatureFile.java,v $
 * Revision 1.6  2006/03/13 15:15:29  linb
 * Changed copyright holder from Crown Copyright to The National Archives.
 * Added reference to licence.txt
 * Changed dates to 2005-2006
 *
 * Revision 1.5  2006/02/07 17:16:22  linb
 * - Change fileReader to ByteReader in formal parameters of methods
 * - use new static constructors
 * - Add detection of if a filePath is a URL or not
 *
 * Revision 1.4  2006/02/07 12:34:57  gaur
 * Removed restriction on priority relationships so that they can be applied between any combination of generic and specific signatures (second recommit because of missing logging)
 *
 *
 * $History: FFSignatureFile.java $
 * 
 * *****************  Version 7  *****************
 * User: Walm         Date: 19/04/05   Time: 18:24
 * Updated in $/PRONOM4/FFIT_SOURCE/signatureFile
 * Provide initial values for version and dateCreated
 * 
 * *****************  Version 6  *****************
 * User: Walm         Date: 18/03/05   Time: 12:39
 * Updated in $/PRONOM4/FFIT_SOURCE/signatureFile
 * add some more exception handling
 * 
 * *****************  Version 5  *****************
 * User: Walm         Date: 15/03/05   Time: 14:39
 * Updated in $/PRONOM4/FFIT_SOURCE/signatureFile
 * fileReader class now holds reference to identificationFile object
 * 
 * *****************  Version 4  *****************
 * User: Mals         Date: 14/03/05   Time: 15:08
 * Updated in $/PRONOM4/FFIT_SOURCE/signatureFile
 * Takes into account of IdentificationFile objects in checkExtension
 * 
 * *****************  Version 3  *****************
 * User: Mals         Date: 14/03/05   Time: 14:30
 * Updated in $/PRONOM4/FFIT_SOURCE/signatureFile
 * runFileIdentification accepts IdentificationFile parameter
 *
 */
package uk.gov.nationalarchives.droid.signatureFile;

import java.util.ArrayList;
import java.util.List;

import uk.gov.nationalarchives.droid.base.DroidConstants;
import uk.gov.nationalarchives.droid.base.FileFormatHit;
import uk.gov.nationalarchives.droid.base.MessageDisplay;
import uk.gov.nationalarchives.droid.base.SimpleElement;
import uk.gov.nationalarchives.droid.binFileReader.ByteReader;


/**
 * holds details of a signature file and uses it to identify binary files
 * 
 * @author Martin Waller
 * @version 4.0.0
 */
public class FFSignatureFile extends SimpleElement {

	private String version = "";
	private String dateCreated = "";
	private FileFormatCollection FFcollection;
	private InternalSignatureCollection intSigs;

	public String getDateCreated() {
		return this.dateCreated;
	}

	public DroidFileFormat getFileFormat(final int theIndex) {
		return (DroidFileFormat) this.FFcollection.getFileFormats().get(theIndex);
	}

	public InternalSignature getInternalSignature(final int theIndex) {
		return this.intSigs.getInternalSignatures().get(theIndex);
	}

	public int getNumFileFormats() {
		return this.FFcollection.getFileFormats().size();
	}

	/* getters */
	public int getNumInternalSignatures() {
		return this.intSigs.getInternalSignatures().size();
	}

	public List<InternalSignature> getSignatures() {
		return this.intSigs.getInternalSignatures();
	}

	public String getVersion() {
		return this.version;
	}

	/**
	 * This method must be run after the signature file data has been read and
	 * before the FFSignatureFile class is used. It points internal signatures
	 * to the fileFormat objects they identify, and it ensures that the sequence
	 * fragments are in the correct order.
	 */
	public void prepareForUse() {
		this.setAllSignatureFileFormats();
		this.reorderAllSequenceFragments();
		this.reorderByteSequences();
	}

	/**
	 * Identify the current file
	 * 
	 * @param targetFile
	 *            The binary file to be identified
	 */
	public synchronized void runFileIdentification(final ByteReader targetFile) {

		final List<InternalSignature> signatureList = getSignatures();
		// record all positive identifications
		for (final InternalSignature internalSig : signatureList) {
			if (internalSig.isFileCompliant(targetFile)) {
				// File matches this internal signature
				targetFile.setPositiveIdent();
				for (int i = 0; i < internalSig.getNumFileFormats(); i++) {
					final FileFormatHit fileHit = new FileFormatHit(
							internalSig.getFileFormat(i),
							DroidConstants.HIT_TYPE_POSITIVE_GENERIC_OR_SPECIFIC,
							internalSig.isSpecific(), "");
					targetFile.addHit(fileHit);
				}
			}
		}

		// remove any hits for which there is a higher priority hit
		if (targetFile.getNumHits() > 1) {
			this.removeLowerPriorityHits(targetFile);
		}

		// carry out file extension checking
		this.checkExtension(targetFile);

		// if there are still no hits then classify as unidentified
		if (targetFile.getNumHits() == 0) {
			targetFile.setNoIdent();
		}
	}

	@Override
	public void setAttributeValue(final String name, final String value) {
		if (name.equals("Version")) {
			setVersion(value.trim());
		} else if (name.equals("DateCreated")) {
			setDateCreated(value);
		} else {
			MessageDisplay.unknownAttributeWarning(name, this.getElementName());
		}
	}

	/* setters */
	public void setFileFormatCollection(final FileFormatCollection coll) {
		this.FFcollection = coll;
	}

	public void setInternalSignatureCollection(
			final InternalSignatureCollection col3) {
		this.intSigs = col3;
	}

	/**
	 * Determines the file extension If the file has got some positive hits,
	 * then check these against this extension If the file has not got any
	 * positive hits, then look for tentative hits based on the extension only.
	 * 
	 * @param targetFile
	 *            The binary file to be identified
	 */
	private void checkExtension(final ByteReader targetFile) {

		// work out if file has an extension
		boolean hasExtension = true;
		final int dotPos = targetFile.getFileName().lastIndexOf(".");
		if (dotPos < 0) {
			hasExtension = false;
		} else if (dotPos == targetFile.getFileName().length() - 1) {
			hasExtension = false;
		} else if (targetFile.getFileName().lastIndexOf("/") > dotPos) {
			hasExtension = false;
		} else if (targetFile.getFileName().lastIndexOf("\\") > dotPos) {
			hasExtension = false;
		}

		//
		if (hasExtension) {
			final String fileExtension = targetFile.getFileName().substring(
					dotPos + 1);

			if (targetFile.getNumHits() > 0) {

				// for each file format which is a hit, check that it expects
				// the given extension - if not give a warning
				for (int iHit = 0; iHit < targetFile.getNumHits(); iHit++) {
					if (!(targetFile.getHit(iHit).getFileFormat()
							.hasMatchingExtension(fileExtension))) {
						targetFile.getHit(iHit).setIdentificationWarning(
								MessageDisplay.FILEEXTENSIONWARNING);
					}
				}// loop through hits

			} else {
				// no positive hits have been found, so search for tenative hits
				// loop through all file formats with no internal signature
				for (int iFormat = 0; iFormat < this.getNumFileFormats(); iFormat++) {
					if (this.getFileFormat(iFormat).getNumInternalSignatures() == 0) {
						if (this.getFileFormat(iFormat).hasMatchingExtension(
								fileExtension)) {
							// add this as a tentative hit
							final FileFormatHit fileHit = new FileFormatHit(
									this.getFileFormat(iFormat),
									DroidConstants.HIT_TYPE_TENTATIVE,
									false, "");
							targetFile.addHit(fileHit);
							targetFile.setTentativeIdent();
						}
					}
				}// loop through file formats
			}
		}// end of if(hasExtension)
		else {
			// if the file does not have an extension then add warning to all
			// its hits
			for (int iHit = 0; iHit < targetFile.getNumHits(); iHit++) {
				targetFile.getHit(iHit).setIdentificationWarning(
						MessageDisplay.FILEEXTENSIONWARNING);
			}
		}
	}

	/**
	 * Remove any hits for which there is a higher priority hit
	 * 
	 * @param targetFile
	 *            The binary file to be identified
	 */
	private void removeLowerPriorityHits(final ByteReader targetFile) {
		// loop through specific hits and list any hits which these have
		// priority over
		final List<Integer> hitsToRemove = new ArrayList<Integer>();
		for (int i = 0; i < targetFile.getNumHits(); i++) {
			for (int j = 0; j < targetFile.getHit(i).getFileFormat()
					.getNumHasPriorityOver(); j++) {
				final int formatID = targetFile.getHit(i).getFileFormat()
						.getHasPriorityOver(j);
				for (int k = 0; k < targetFile.getNumHits(); k++) { // loop
																	// through
																	// hits to
																	// find any
																	// for this
																	// file
																	// format
					if (targetFile.getHit(k).getFileFormat().getID() == formatID) {
						hitsToRemove.add(new Integer(k)); // use string
															// representation as
															// ArrayList won't
															// take integers
						break;
					}
				}
			}
		}
		// Create sorted array of indexes for hits to be removed
		final int[] indexesOfHits = new int[hitsToRemove.size()];
		int numHitsToRemove = 0;
		for (int i = 0; i < hitsToRemove.size(); i++) { // loop through unsorted
														// list of hits to be
														// removed
			int j = numHitsToRemove;
			final int indexOfHit = hitsToRemove.get(i);
			while (j > 0 && indexesOfHits[j - 1] > indexOfHit) {
				indexesOfHits[j] = indexesOfHits[j - 1];
				--j;
			}
			indexesOfHits[j] = indexOfHit;
			++numHitsToRemove;
		}
		// Delete hits in decreasing index order, ignorinmg any repetitions
		for (int i = indexesOfHits.length - 1; i >= 0; i--) {
			if (i == (indexesOfHits.length - 1)) {
				targetFile.removeHit(indexesOfHits[i]);
			} else if (indexesOfHits[i] != indexesOfHits[i + 1]) {
				targetFile.removeHit(indexesOfHits[i]);
			}
		}

	}

	/**
	 * Run prepareSeqFragments on all subSequences within all ByteSequences
	 * within all internalSignatures.
	 */
	private void reorderAllSequenceFragments() {
		for (int iSig = 0; iSig < this.getNumInternalSignatures(); iSig++) {
			for (int iBS = 0; iBS < this.getInternalSignature(iSig)
					.getNumByteSequences(); iBS++) {
				for (int iSS = 0; iSS < this.getInternalSignature(iSig)
						.getByteSequence(iBS).getNumSubSequences(); iSS++) {
					this.getInternalSignature(iSig).getByteSequence(iBS)
							.getSubSequence(iSS).prepareSeqFragments();
				}
			}
		}
	}

	/**
	 * Ensure that the BOFs and EOFs are searched for before the variable
	 * position byte sequences
	 * 
	 */
	private void reorderByteSequences() {
		for (int iSig = 0; iSig < this.getNumInternalSignatures(); iSig++) {
			final InternalSignature sig = this.getInternalSignature(iSig);
			final List<ByteSequence> BOFoffsetByteSequences = new ArrayList<ByteSequence>();
			final List<ByteSequence> EOFoffsetByteSequences = new ArrayList<ByteSequence>();
			final List<ByteSequence> variableByteSequences = new ArrayList<ByteSequence>();
			for (int iBS = 0; iBS < sig.getNumByteSequences(); iBS++) {
				final ByteSequence seq = sig.getByteSequence(iBS);
				if (seq.getReference().startsWith("BOF")) {
					BOFoffsetByteSequences.add(seq);
				}
				if (seq.getReference().startsWith("EOF")) {
					EOFoffsetByteSequences.add(seq);
				} else {
					variableByteSequences.add(seq);
				}
			}
			final List<ByteSequence> byteSequences = new ArrayList<ByteSequence>();
			byteSequences.addAll(BOFoffsetByteSequences);
			byteSequences.addAll(EOFoffsetByteSequences);
			byteSequences.addAll(variableByteSequences);
			sig.resetByteSequences(byteSequences);
		}
	}

	/**
	 * Points all internal signatures to the fileFormat objects they identify.
	 */
	private void setAllSignatureFileFormats() {
		for (int iFormat = 0; iFormat < this.getNumFileFormats(); iFormat++) { // loop
																				// through
																				// file
																				// formats
			for (int iFileSig = 0; iFileSig < this.getFileFormat(iFormat)
					.getNumInternalSignatures(); iFileSig++) { // loop through
																// internal
																// signatures
																// for each file
																// format
				final int iFileSigID = this.getFileFormat(iFormat)
						.getInternalSignatureID(iFileSig);
				// loop through all internal signatures to find one with a
				// matching ID
				for (int iIntSig = 0; iIntSig < this.getNumInternalSignatures(); iIntSig++) {
					if (this.getInternalSignature(iIntSig).getID() == iFileSigID) {
						this.getInternalSignature(iIntSig).addFileFormat(
								this.getFileFormat(iFormat));
						break;
					}
				}
			}
		}
	}

	private void setDateCreated(final String dc) {
		this.dateCreated = dc;
	}

	private void setVersion(final String vers) {
		this.version = vers;
	}
}