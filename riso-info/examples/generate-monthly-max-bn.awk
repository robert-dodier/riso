BEGIN { first_day = 182; ndays = 31; daily_max_bn = "daily-max"; host="beethoven"; }
END { printf("riso.belief_nets.BeliefNetwork monthly-max\n");
  printf("{\n\triso.belief_nets.Variable peak\n\t{\n\t\tparents\n\t\t{\n");
  for ( i=first_day; i<first_day+ndays; i++ ) printf("\t\t\t%s/%s[%d].peak\n",host,daily_max_bn,i);
  printf("\t\t}\n\t\tdistribution riso.distributions.Max { }\n\t}\n\n");

  printf("\triso.belief_nets.Variable mid-peak\n\t{\n\t\tparents\n\t\t{\n");
  for ( i=first_day; i<first_day+ndays; i++ ) printf("\t\t\t%s/%s[%d].mid-peak\n",host,daily_max_bn,i);
  printf("\t\t}\n\t\tdistribution riso.distributions.Max { }\n\t}\n\n");

  printf("\triso.belief_nets.Variable off-peak\n\t{\n\t\tparents\n\t\t{\n");
  for ( i=first_day; i<first_day+ndays; i++ ) printf("\t\t\t%s/%s[%d].off-peak\n",host,daily_max_bn,i);
  printf("\t\t}\n\t\tdistribution riso.distributions.Max { }\n\t}\n");

  printf("}\n");
}
