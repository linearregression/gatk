package org.broadinstitute.sting.gatk.walkers.recalibration;

import org.broadinstitute.sting.WalkerTest;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.io.File;

public class RecalibrationWalkersIntegrationTest extends WalkerTest {
    static HashMap<String, String> paramsFiles = new HashMap<String, String>();
    static HashMap<String, String> paramsFilesNoReadGroupTest = new HashMap<String, String>();

    @Test
    public void testCountCovariates1() {
        HashMap<String, String> e = new HashMap<String, String>();
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12892.SLX.SRP000031.2009_06.selected.bam", "9eddef42ae847f2edd966d3a5b463e50" );
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.SOLID.bam", "22ee532c7e521d8cbfabedb07673429d");
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12873.454.SRP000031.2009_06.chr1.10_20mb.bam", "8b91c48f186c1e44723ac4e047ac553c" );
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.allTechs.bam", "f586fcd8642deae6504d39430b20b713" );

        for ( Map.Entry<String, String> entry : e.entrySet() ) {
            String bam = entry.getKey();
            String md5 = entry.getValue();

            WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                    "-R /broad/1KG/reference/human_b36_both.fasta" +
                            " --DBSNP /humgen/gsa-scr1/GATK_Data/dbsnp_129_b36.rod" +
                            " -T CountCovariates" +
                            " -I " + bam +
                            ( bam.equals( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.allTechs.bam" )
                                ? " -L 1:10,800,000-10,810,000" : " -L 1:10,000,000-10,200,000" ) +
                            " -cov ReadGroupCovariate" +
                            " -cov QualityScoreCovariate" +
                            " -cov CycleCovariate" +
                            " -cov DinucCovariate" +
                            " --sorted_output" +
                            " --solid_recal_mode SET_Q_ZERO" +
                            " -recalFile %s",
                    1, // just one output file
                    Arrays.asList(md5));
            List<File> result = executeTest("testCountCovariates1", spec).getFirst();
            paramsFiles.put(bam, result.get(0).getAbsolutePath());
        }
    }
    
    @Test
    public void testTableRecalibrator1() {
        HashMap<String, String> e = new HashMap<String, String>();
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12892.SLX.SRP000031.2009_06.selected.bam", "a3d4153765625a451bee768ad46775b3" );
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.SOLID.bam", "5954b7246d2e0c5c65267026819d5933");
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12873.454.SRP000031.2009_06.chr1.10_20mb.bam", "91fd3c557db56fd969523d8c06d71a0b" );
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.allTechs.bam", "9655f57949b01d6ee8a7c9b92e61a47f" );

        for ( Map.Entry<String, String> entry : e.entrySet() ) {
            String bam = entry.getKey();
            String md5 = entry.getValue();
            String paramsFile = paramsFiles.get(bam);
            System.out.printf("PARAMS FOR %s is %s%n", bam, paramsFile);
            if ( paramsFile != null ) {
                WalkerTestSpec spec = new WalkerTestSpec(
                        "-R /broad/1KG/reference/human_b36_both.fasta" +
                                " -T TableRecalibration" +
                                " -I " + bam +
                                ( bam.equals( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.allTechs.bam" )
                                    ? " -L 1:10,800,000-10,810,000" : " -L 1:10,100,000-10,300,000" ) +
                                " -outputBam %s" +
                                " --no_pg_tag" +
                                " --solid_recal_mode SET_Q_ZERO" +
                                " -recalFile " + paramsFile,
                        1, // just one output file
                        Arrays.asList(md5));
                executeTest("testTableRecalibrator1", spec);
            }
        }
    }

    @Test
    public void testCountCovariatesVCF() {
        HashMap<String, String> e = new HashMap<String, String>();
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.SOLID.bam", "79e6738031f5a6f993838b33d70716c3");

        for ( Map.Entry<String, String> entry : e.entrySet() ) {
            String bam = entry.getKey();
            String md5 = entry.getValue();

            WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                    "-R /broad/1KG/reference/human_b36_both.fasta" +
                            " -B dbsnp,VCF,/humgen/gsa-scr1/GATK_Data/Validation_Data/vcfexample3.vcf" +
                            " -T CountCovariates" +
                            " -I " + bam +
                            " -L 1:10,000,000-10,200,000" +
                            " -cov ReadGroupCovariate" +
                            " -cov QualityScoreCovariate" +
                            " -cov CycleCovariate" +
                            " -cov DinucCovariate" +
                            " --sorted_output" +
                            " --solid_recal_mode SET_Q_ZERO" +
                            " -recalFile %s",
                    1, // just one output file
                    Arrays.asList(md5));
            executeTest("testCountCovariatesVCF", spec);
        }
    }

    @Test
    public void testCountCovariatesNoReadGroups() {
        HashMap<String, String> e = new HashMap<String, String>();
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12762.SOLID.SRP000031.2009_07.chr1.10_20mb.bam", "d6f3ea0cf2129ca5488977f087f1aa25" );

        for ( Map.Entry<String, String> entry : e.entrySet() ) {
            String bam = entry.getKey();
            String md5 = entry.getValue();

            WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                    "-R /broad/1KG/reference/human_b36_both.fasta" +
                            " --DBSNP /humgen/gsa-scr1/GATK_Data/dbsnp_129_b36.rod" +
                            " -T CountCovariates" +
                            " -I " + bam +
                            " -L 1:10,000,000-10,200,000" +
                            " -cov ReadGroupCovariate" +
                            " -cov QualityScoreCovariate" +
                            " -cov CycleCovariate" +
                            " -cov DinucCovariate" +
                            " --default_platform illumina" +
                            " --sorted_output" +
                            " --solid_recal_mode SET_Q_ZERO" +
                            " -recalFile %s",
                    1, // just one output file
                    Arrays.asList(md5));
            List<File> result = executeTest("testCountCovariatesNoReadGroups", spec).getFirst();
            paramsFilesNoReadGroupTest.put(bam, result.get(0).getAbsolutePath());
        }
    }

    @Test
    public void testTableRecalibratorNoReadGroups() {
        HashMap<String, String> e = new HashMap<String, String>();
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12762.SOLID.SRP000031.2009_07.chr1.10_20mb.bam", "c0d5fdd6e07395ae2d7b08431995b30d" );

        for ( Map.Entry<String, String> entry : e.entrySet() ) {
            String bam = entry.getKey();
            String md5 = entry.getValue();
            String paramsFile = paramsFilesNoReadGroupTest.get(bam);
            System.out.printf("PARAMS FOR %s is %s%n", bam, paramsFile);
            if ( paramsFile != null ) {
                WalkerTestSpec spec = new WalkerTestSpec(
                        "-R /broad/1KG/reference/human_b36_both.fasta" +
                                " -T TableRecalibration" +
                                " -I " + bam +
                                " -L 1:10,100,000-10,300,000" +
                                " -outputBam %s" +
                                " --no_pg_tag" +
                                " --solid_recal_mode SET_Q_ZERO" +
                                " --default_platform illumina" +
                                " -recalFile " + paramsFile,
                        1, // just one output file
                        Arrays.asList(md5));
                executeTest("testTableRecalibratorNoReadGroups", spec);
            }
        }
    }

    @Test
    public void testCountCovariatesNoIndex() {
        HashMap<String, String> e = new HashMap<String, String>();
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.allTechs.noindex.bam", "f5a0adce5458ab661029fa5056a55551" );

        for ( Map.Entry<String, String> entry : e.entrySet() ) {
            String bam = entry.getKey();
            String md5 = entry.getValue();

            WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                    "-R /broad/1KG/reference/human_b36_both.fasta" +
                            " --DBSNP /humgen/gsa-scr1/GATK_Data/dbsnp_129_b36.rod" +
                            " -T CountCovariates" +
                            " -I " + bam +
                            " -cov ReadGroupCovariate" +
                            " -cov QualityScoreCovariate" +
                            " --sorted_output" +
                            " --solid_recal_mode DO_NOTHING" +
                            " -recalFile %s" +
                            " -U",
                    1, // just one output file
                    Arrays.asList(md5));
            List<File> result = executeTest("testCountCovariatesNoIndex", spec).getFirst();
            paramsFiles.put(bam, result.get(0).getAbsolutePath());
        }
    }

    @Test
    public void testTableRecalibratorNoIndex() {
        HashMap<String, String> e = new HashMap<String, String>();
        e.put( "/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.allTechs.noindex.bam", "33cb41fa3e959ac15ac9af329167a736" );

        for ( Map.Entry<String, String> entry : e.entrySet() ) {
            String bam = entry.getKey();
            String md5 = entry.getValue();
            String paramsFile = paramsFiles.get(bam);
            System.out.printf("PARAMS FOR %s is %s%n", bam, paramsFile);
            if ( paramsFile != null ) {
                WalkerTestSpec spec = new WalkerTestSpec(
                        "-R /broad/1KG/reference/human_b36_both.fasta" +
                                " -T TableRecalibration" +
                                " -I " + bam +
                                " -outputBam %s" +
                                " --no_pg_tag" +
                                " --solid_recal_mode DO_NOTHING" +
                                " -recalFile " + paramsFile +
                                " -U",
                        1, // just one output file
                        Arrays.asList(md5));
                executeTest("testTableRecalibratorNoIndex", spec);
            }
        }
    }

}
