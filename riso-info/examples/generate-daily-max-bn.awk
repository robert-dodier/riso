BEGIN { nday = 1; elec_bn = "electrical-demand"; total_elec_var = "electrical-demand"; host = "sonero"; }
END { printf("riso.belief_nets.BeliefNetwork daily-max[%d]\n",nday);
  printf("{\n\triso.belief_nets.Variable peak\n\t{\n\t\tparents\n\t\t{\n");
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+9, total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+10,total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+11,total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+12,total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+13,total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+14,total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+15,total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+16,total_elec_var);
  printf("\t\t}\n\t\tdistribution riso.distributions.Max { }\n\t}\n\n");

  printf("\triso.belief_nets.Variable mid-peak\n\t{\n\t\tparents\n\t\t{\n");
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+7, total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+8, total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+17,total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+18,total_elec_var);
  printf("\t\t}\n\t\tdistribution riso.distributions.Max { }\n\t}\n\n");

  printf("\triso.belief_nets.Variable off-peak\n\t{\n\t\tparents\n\t\t{\n");
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+0, total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+1, total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+2, total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+3, total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+4, total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+5, total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+6, total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+19,total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+20,total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+21,total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+22,total_elec_var);
  printf("\t\t\t%s/%s.slice[%d].%s\n",host,elec_bn,(nday-1)*24+23,total_elec_var);
  printf("\t\t}\n\t\tdistribution riso.distributions.Max { }\n\t}\n");

  printf("}\n");
}
