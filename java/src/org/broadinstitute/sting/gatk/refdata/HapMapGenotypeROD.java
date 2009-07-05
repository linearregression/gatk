package org.broadinstitute.sting.gatk.refdata;

import java.util.*;

import org.broadinstitute.sting.utils.GenomeLoc;
import org.broadinstitute.sting.utils.GenomeLocParser;

public class HapMapGenotypeROD extends TabularROD 
{
    public HapMapGenotypeROD(final String name) 
	{
        super(name);
    }

    public GenomeLoc getLocation() 
	{
		//System.out.printf("chrom: %s; pos: %s\n", this.get("chrom"), this.get("pos"));
       
		return GenomeLocParser.createGenomeLoc(this.get("chrom").replaceAll("chr", ""), Long.parseLong(this.get("pos")));
    }

	public String[] getSampleIDs()
	{
		ArrayList<String> header = this.getHeader();
		String[] sample_ids = new String[header.size()-11];
		for (int i = 11; i < header.size(); i++)
		{
			sample_ids[i-11] = header.get(i);
		}
		return sample_ids;
	}

	public String[] getGenotypes()
	{
		ArrayList<String> header = this.getHeader();
		String[] genotypes = new String[header.size()-11];
		for (int i = 11; i < header.size(); i++)
		{
			genotypes[i-11] = this.get(header.get(i));
		}
		return genotypes;
	}

}
